package com.swiftpay.analytics.repository;

import com.swiftpay.analytics.entity.PaymentAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface PaymentAnalyticsRepository extends JpaRepository<PaymentAnalytics, UUID> {

    long countByStatus(String status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentAnalytics p WHERE p.status = 'COMPLETED'")
    BigDecimal sumCompletedAmount();

    @Query("SELECT COUNT(p) FROM PaymentAnalytics p WHERE p.processedAt >= :since AND p.status = 'COMPLETED'")
    long countCompletedSince(LocalDateTime since);
}
