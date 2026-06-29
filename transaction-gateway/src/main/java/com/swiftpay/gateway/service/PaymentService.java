package com.swiftpay.gateway.service;

import com.swiftpay.gateway.dto.PaymentInitiatedEvent;
import com.swiftpay.gateway.dto.PaymentRequest;
import com.swiftpay.gateway.dto.PaymentResponse;
import com.swiftpay.gateway.entity.Account;
import com.swiftpay.gateway.entity.Payment;
import com.swiftpay.gateway.entity.PaymentStatus;
import com.swiftpay.gateway.exception.AccountNotFoundException;
import com.swiftpay.gateway.exception.DuplicateTransactionException;
import com.swiftpay.gateway.exception.InsufficientFundsException;
import com.swiftpay.gateway.kafka.PaymentEventProducer;
import com.swiftpay.gateway.repository.AccountRepository;
import com.swiftpay.gateway.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final AccountRepository accountRepository;
    private final IdempotencyService idempotencyService;
    private final PaymentEventProducer eventProducer;

    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request) {
        // resolve or generate idempotency key
        String idempotencyKey = StringUtils.hasText(request.getIdempotencyKey())
                ? request.getIdempotencyKey()
                : UUID.randomUUID().toString();

        // check for duplicate in redis before doing anything else
        if (!idempotencyService.tryAcquire(idempotencyKey)) {
            throw new DuplicateTransactionException(idempotencyKey);
        }

        // validate sender
        Account sender = accountRepository.findByUserId(request.getSenderId())
                .orElseThrow(() -> new AccountNotFoundException(request.getSenderId()));

        // validate receiver exists too, otherwise we'd only find out at ledger processing time
        accountRepository.findByUserId(request.getReceiverId())
                .orElseThrow(() -> new AccountNotFoundException(request.getReceiverId()));

        // balance check - try cache first, fallback to DB
        BigDecimal senderBalance = getEffectiveBalance(request.getSenderId(), sender.getBalance());
        if (senderBalance.compareTo(request.getAmount()) < 0) {
            idempotencyService.evictBalance(request.getSenderId());
            throw new InsufficientFundsException(request.getSenderId());
        }

        // save as PENDING and fire kafka event
        String transactionId = "txn-" + UUID.randomUUID();
        Payment payment = Payment.builder()
                .transactionId(transactionId)
                .senderId(request.getSenderId())
                .receiverId(request.getReceiverId())
                .amount(request.getAmount())
                .currency(request.getCurrency().toUpperCase())
                .status(PaymentStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .build();

        payment = paymentRepository.save(payment);
        log.info("saved PENDING payment: txn={}", transactionId);

        PaymentInitiatedEvent event = PaymentInitiatedEvent.builder()
                .transactionId(transactionId)
                .senderId(request.getSenderId())
                .receiverId(request.getReceiverId())
                .amount(request.getAmount())
                .currency(request.getCurrency().toUpperCase())
                .initiatedAt(LocalDateTime.now())
                .build();

        eventProducer.publishPaymentInitiated(event);

        return PaymentResponse.builder()
                .transactionId(transactionId)
                .status(payment.getStatus().name())
                .senderId(payment.getSenderId())
                .receiverId(payment.getReceiverId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .createdAt(payment.getCreatedAt())
                .message("Payment initiated successfully and is being processed.")
                .build();
    }

    public PaymentResponse getPaymentStatus(String transactionId) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

        return PaymentResponse.builder()
                .transactionId(payment.getTransactionId())
                .status(payment.getStatus().name())
                .senderId(payment.getSenderId())
                .receiverId(payment.getReceiverId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .createdAt(payment.getCreatedAt())
                .message(payment.getFailureReason() != null ? payment.getFailureReason() : "")
                .build();
    }

    private BigDecimal getEffectiveBalance(String userId, BigDecimal dbBalance) {
        String cached = idempotencyService.getCachedBalance(userId);
        if (cached != null) {
            return new BigDecimal(cached);
        }
        // TODO: maybe move cache TTL to config instead of hardcoding 30s
        // Cache for short window to reduce DB load under high TPS
        idempotencyService.cacheBalance(userId, dbBalance.toPlainString());
        return dbBalance;
    }
}
