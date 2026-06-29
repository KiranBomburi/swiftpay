package com.swiftpay.ledger.service;

import com.swiftpay.ledger.dto.PaymentInitiatedEvent;
import com.swiftpay.ledger.dto.PaymentResultEvent;
import com.swiftpay.ledger.dto.TransactionHistoryResponse;
import com.swiftpay.ledger.entity.EntryType;
import com.swiftpay.ledger.entity.LedgerAccount;
import com.swiftpay.ledger.entity.LedgerEntry;
import com.swiftpay.ledger.repository.LedgerAccountRepository;
import com.swiftpay.ledger.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerAccountRepository accountRepository;
    private final LedgerEntryRepository entryRepository;

    /**
     * Atomically debits the sender and credits the receiver.
     * Uses pessimistic locking on both accounts to prevent concurrent corruption.
     * Returns a result event to be published back to Kafka.
     */
    @Transactional
    public PaymentResultEvent processPayment(PaymentInitiatedEvent event) {
        log.info("processing payment: txn={}, amount={}", event.getTransactionId(), event.getAmount());

        // lock accounts in alphabetical order to avoid deadlocks when two transfers
        // happen between same pair simultaneously
        String firstLock  = event.getSenderId().compareTo(event.getReceiverId()) < 0
                            ? event.getSenderId() : event.getReceiverId();
        String secondLock = firstLock.equals(event.getSenderId())
                            ? event.getReceiverId() : event.getSenderId();

        LedgerAccount first  = accountRepository.findByUserIdWithLock(firstLock).orElse(null);
        LedgerAccount second = accountRepository.findByUserIdWithLock(secondLock).orElse(null);

        LedgerAccount sender   = firstLock.equals(event.getSenderId()) ? first : second;
        LedgerAccount receiver = firstLock.equals(event.getSenderId()) ? second : first;

        if (sender == null || receiver == null) {
            String reason = sender == null
                    ? "Sender account not found: " + event.getSenderId()
                    : "Receiver account not found: " + event.getReceiverId();
            log.error("account lookup failed: {}", reason);
            // TODO: should we publish a PaymentFailed event here too? check with team
            return buildResultEvent(event, "FAILED", reason);
        }

        // re-check balance at ledger time — gateway check is best-effort with cache
        if (sender.getBalance().compareTo(event.getAmount()) < 0) {
            String reason = "Insufficient funds at ledger processing time for: " + event.getSenderId();
            log.warn(reason);
            recordFailedEntries(event, sender, receiver, reason);
            return buildResultEvent(event, "FAILED", reason);
        }

        // do the actual transfer
        BigDecimal senderBalanceAfter = sender.getBalance().subtract(event.getAmount());
        sender.setBalance(senderBalanceAfter);
        accountRepository.save(sender);

        BigDecimal receiverBalanceAfter = receiver.getBalance().add(event.getAmount());
        receiver.setBalance(receiverBalanceAfter);
        accountRepository.save(receiver);

        // write double-entry records
        entryRepository.save(LedgerEntry.builder()
                .transactionId(event.getTransactionId())
                .userId(event.getSenderId())
                .entryType(EntryType.DEBIT)
                .amount(event.getAmount())
                .balanceAfter(senderBalanceAfter)
                .currency(event.getCurrency())
                .status("COMPLETED")
                .build());

        entryRepository.save(LedgerEntry.builder()
                .transactionId(event.getTransactionId())
                .userId(event.getReceiverId())
                .entryType(EntryType.CREDIT)
                .amount(event.getAmount())
                .balanceAfter(receiverBalanceAfter)
                .currency(event.getCurrency())
                .status("COMPLETED")
                .build());

        log.info("transfer done: txn={}, sender_balance={}, receiver_balance={}",
                event.getTransactionId(), senderBalanceAfter, receiverBalanceAfter);

        return buildResultEvent(event, "COMPLETED", null);
    }

    public List<TransactionHistoryResponse> getTransactionHistory(String userId) {
        return entryRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(entry -> TransactionHistoryResponse.builder()
                        .transactionId(entry.getTransactionId())
                        .entryType(entry.getEntryType().name())
                        .amount(entry.getAmount())
                        .balanceAfter(entry.getBalanceAfter())
                        .currency(entry.getCurrency())
                        .status(entry.getStatus())
                        .createdAt(entry.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private void recordFailedEntries(PaymentInitiatedEvent event, LedgerAccount sender,
                                     LedgerAccount receiver, String reason) {
        entryRepository.save(LedgerEntry.builder()
                .transactionId(event.getTransactionId())
                .userId(event.getSenderId())
                .entryType(EntryType.DEBIT)
                .amount(event.getAmount())
                .balanceAfter(sender.getBalance())
                .currency(event.getCurrency())
                .status("FAILED")
                .failureReason(reason)
                .build());
    }

    private PaymentResultEvent buildResultEvent(PaymentInitiatedEvent event,
                                                 String status, String reason) {
        return PaymentResultEvent.builder()
                .transactionId(event.getTransactionId())
                .senderId(event.getSenderId())
                .receiverId(event.getReceiverId())
                .amount(event.getAmount())
                .currency(event.getCurrency())
                .status(status)
                .failureReason(reason)
                .processedAt(LocalDateTime.now())
                .build();
    }
}
