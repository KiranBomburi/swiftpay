package com.swiftpay.analytics.service;

import com.swiftpay.analytics.dto.PaymentResultEvent;
import com.swiftpay.analytics.entity.PaymentAnalytics;
import com.swiftpay.analytics.repository.PaymentAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsService {

    private final PaymentAnalyticsRepository analyticsRepository;

    @Transactional
    public void ingestPaymentEvent(PaymentResultEvent event) {
        PaymentAnalytics record = PaymentAnalytics.builder()
                .transactionId(event.getTransactionId())
                .senderId(event.getSenderId())
                .receiverId(event.getReceiverId())
                .amount(event.getAmount())
                .currency(event.getCurrency())
                .status(event.getStatus())
                .processedAt(event.getProcessedAt() != null ? event.getProcessedAt() : LocalDateTime.now())
                .build();

        analyticsRepository.save(record);
        log.info("Analytics ingested: txn={}, status={}, amount={}",
                event.getTransactionId(), event.getStatus(), event.getAmount());
    }

    public Map<String, Object> getVolumeSummary() {
        long completed = analyticsRepository.countByStatus("COMPLETED");
        long failed    = analyticsRepository.countByStatus("FAILED");
        BigDecimal totalVolume = analyticsRepository.sumCompletedAmount();
        long last5min  = analyticsRepository.countCompletedSince(LocalDateTime.now().minusMinutes(5));

        return Map.of(
            "total_completed", completed,
            "total_failed", failed,
            "total_volume_inr", totalVolume,
            "completed_last_5min", last5min
        );
    }
}
