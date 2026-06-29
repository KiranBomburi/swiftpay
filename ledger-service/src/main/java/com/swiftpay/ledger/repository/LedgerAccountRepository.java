package com.swiftpay.ledger.repository;

import com.swiftpay.ledger.entity.LedgerAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, UUID> {

    Optional<LedgerAccount> findByUserId(String userId);

    // Pessimistic write lock — guarantees no concurrent balance updates for same user
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM LedgerAccount a WHERE a.userId = :userId")
    Optional<LedgerAccount> findByUserIdWithLock(@Param("userId") String userId);
}
