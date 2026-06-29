package com.swiftpay.gateway.service;

import com.swiftpay.gateway.dto.PaymentRequest;
import com.swiftpay.gateway.dto.PaymentResponse;
import com.swiftpay.gateway.entity.Account;
import com.swiftpay.gateway.entity.Payment;
import com.swiftpay.gateway.entity.PaymentStatus;
import com.swiftpay.gateway.exception.DuplicateTransactionException;
import com.swiftpay.gateway.exception.InsufficientFundsException;
import com.swiftpay.gateway.kafka.PaymentEventProducer;
import com.swiftpay.gateway.repository.AccountRepository;
import com.swiftpay.gateway.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private IdempotencyService idempotencyService;
    @Mock private PaymentEventProducer eventProducer;

    @InjectMocks private PaymentService paymentService;

    private Account senderAccount;
    private Account receiverAccount;

    @BeforeEach
    void setUp() {
        senderAccount = Account.builder()
                .id(UUID.randomUUID())
                .userId("user_001")
                .balance(new BigDecimal("10000.00"))
                .currency("INR")
                .build();

        receiverAccount = Account.builder()
                .id(UUID.randomUUID())
                .userId("user_002")
                .balance(new BigDecimal("5000.00"))
                .currency("INR")
                .build();
    }

    @Test
    void initiatePayment_success() {
        PaymentRequest req = buildRequest("user_001", "user_002", "500.00", "INR");

        when(idempotencyService.tryAcquire(any())).thenReturn(true);
        when(accountRepository.findByUserId("user_001")).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByUserId("user_002")).thenReturn(Optional.of(receiverAccount));
        when(idempotencyService.getCachedBalance(any())).thenReturn(null);

        Payment savedPayment = Payment.builder()
                .transactionId("txn-abc")
                .senderId("user_001")
                .receiverId("user_002")
                .amount(new BigDecimal("500.00"))
                .currency("INR")
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        when(paymentRepository.save(any())).thenReturn(savedPayment);

        PaymentResponse response = paymentService.initiatePayment(req);

        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getSenderId()).isEqualTo("user_001");
        verify(eventProducer, times(1)).publishPaymentInitiated(any());
    }

    @Test
    void initiatePayment_insufficientFunds_throwsException() {
        PaymentRequest req = buildRequest("user_001", "user_002", "99999.00", "INR");

        when(idempotencyService.tryAcquire(any())).thenReturn(true);
        when(accountRepository.findByUserId("user_001")).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByUserId("user_002")).thenReturn(Optional.of(receiverAccount));
        when(idempotencyService.getCachedBalance(any())).thenReturn(null);

        assertThatThrownBy(() -> paymentService.initiatePayment(req))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("user_001");

        verify(eventProducer, never()).publishPaymentInitiated(any());
    }

    @Test
    void initiatePayment_duplicateIdempotencyKey_throwsException() {
        PaymentRequest req = buildRequest("user_001", "user_002", "100.00", "INR");
        req.setIdempotencyKey("duplicate-key");

        when(idempotencyService.tryAcquire("duplicate-key")).thenReturn(false);

        assertThatThrownBy(() -> paymentService.initiatePayment(req))
                .isInstanceOf(DuplicateTransactionException.class);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void initiatePayment_usesRedisBalanceCache() {
        PaymentRequest req = buildRequest("user_001", "user_002", "500.00", "INR");

        when(idempotencyService.tryAcquire(any())).thenReturn(true);
        when(accountRepository.findByUserId("user_001")).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByUserId("user_002")).thenReturn(Optional.of(receiverAccount));
        // Simulate cached balance
        when(idempotencyService.getCachedBalance("user_001")).thenReturn("10000.00");

        Payment savedPayment = Payment.builder()
                .transactionId("txn-xyz")
                .senderId("user_001")
                .receiverId("user_002")
                .amount(new BigDecimal("500.00"))
                .currency("INR")
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        when(paymentRepository.save(any())).thenReturn(savedPayment);

        PaymentResponse response = paymentService.initiatePayment(req);
        assertThat(response.getStatus()).isEqualTo("PENDING");
    }

    private PaymentRequest buildRequest(String sender, String receiver, String amount, String currency) {
        PaymentRequest req = new PaymentRequest();
        req.setSenderId(sender);
        req.setReceiverId(receiver);
        req.setAmount(new BigDecimal(amount));
        req.setCurrency(currency);
        return req;
    }
}
