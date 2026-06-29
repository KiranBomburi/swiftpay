package com.swiftpay.ledger.service;

import com.swiftpay.ledger.dto.PaymentInitiatedEvent;
import com.swiftpay.ledger.dto.PaymentResultEvent;
import com.swiftpay.ledger.entity.LedgerAccount;
import com.swiftpay.ledger.repository.LedgerAccountRepository;
import com.swiftpay.ledger.repository.LedgerEntryRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock private LedgerAccountRepository accountRepository;
    @Mock private LedgerEntryRepository entryRepository;

    @InjectMocks private LedgerService ledgerService;

    private LedgerAccount senderAccount;
    private LedgerAccount receiverAccount;

    @BeforeEach
    void setUp() {
        senderAccount = LedgerAccount.builder()
                .id(UUID.randomUUID())
                .userId("user_001")
                .balance(new BigDecimal("10000.00"))
                .currency("INR")
                .version(0L)
                .build();

        receiverAccount = LedgerAccount.builder()
                .id(UUID.randomUUID())
                .userId("user_002")
                .balance(new BigDecimal("5000.00"))
                .currency("INR")
                .version(0L)
                .build();
    }

    @Test
    void processPayment_success_debitsAndCredits() {
        when(accountRepository.findByUserIdWithLock("user_001")).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByUserIdWithLock("user_002")).thenReturn(Optional.of(receiverAccount));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentInitiatedEvent event = buildEvent("user_001", "user_002", "1000.00");
        PaymentResultEvent result = ledgerService.processPayment(event);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(senderAccount.getBalance()).isEqualByComparingTo("9000.00");
        assertThat(receiverAccount.getBalance()).isEqualByComparingTo("6000.00");
        verify(entryRepository, times(2)).save(any());
    }

    @Test
    void processPayment_insufficientFunds_returnsFailed() {
        when(accountRepository.findByUserIdWithLock("user_001")).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByUserIdWithLock("user_002")).thenReturn(Optional.of(receiverAccount));

        PaymentInitiatedEvent event = buildEvent("user_001", "user_002", "99999.00");
        PaymentResultEvent result = ledgerService.processPayment(event);

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getFailureReason()).contains("Insufficient funds");
        verify(accountRepository, never()).save(senderAccount); // no update to sender
    }

    @Test
    void processPayment_senderNotFound_returnsFailed() {
        when(accountRepository.findByUserIdWithLock("user_001")).thenReturn(Optional.empty());
        when(accountRepository.findByUserIdWithLock("user_002")).thenReturn(Optional.of(receiverAccount));

        PaymentInitiatedEvent event = buildEvent("user_001", "user_002", "100.00");
        PaymentResultEvent result = ledgerService.processPayment(event);

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getFailureReason()).contains("not found");
    }

    private PaymentInitiatedEvent buildEvent(String sender, String receiver, String amount) {
        return PaymentInitiatedEvent.builder()
                .transactionId("txn-test-" + UUID.randomUUID())
                .senderId(sender)
                .receiverId(receiver)
                .amount(new BigDecimal(amount))
                .currency("INR")
                .initiatedAt(LocalDateTime.now())
                .build();
    }
}
