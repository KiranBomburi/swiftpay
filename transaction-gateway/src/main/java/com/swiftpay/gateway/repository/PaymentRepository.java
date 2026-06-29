package com.swiftpay.gateway.repository;

import com.swiftpay.gateway.entity.Payment;
import com.swiftpay.gateway.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByTransactionId(String transactionId);

    List<Payment> findBySenderIdOrderByCreatedAtDesc(String senderId);

    List<Payment> findByReceiverIdOrderByCreatedAtDesc(String receiverId);

    @Modifying
    @Query("UPDATE Payment p SET p.status = :status, p.failureReason = :reason WHERE p.transactionId = :txnId")
    int updateStatusByTransactionId(@Param("txnId") String transactionId,
                                    @Param("status") PaymentStatus status,
                                    @Param("reason") String reason);
}
