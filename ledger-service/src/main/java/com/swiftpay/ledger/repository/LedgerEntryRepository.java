package com.swiftpay.ledger.repository;

import com.swiftpay.ledger.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    List<LedgerEntry> findByUserIdOrderByCreatedAtDesc(String userId);

    List<LedgerEntry> findByTransactionIdOrderByCreatedAtAsc(String transactionId);
}
