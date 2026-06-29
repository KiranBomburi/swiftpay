package com.swiftpay.analytics.consumer;

import com.swiftpay.analytics.dto.PaymentResultEvent;
import com.swiftpay.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentCompletedConsumer {

    private final AnalyticsService analyticsService;

    @KafkaListener(
        topics = "${swiftpay.kafka.topics.payment-completed}",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onPaymentCompleted(@Payload PaymentResultEvent event,
                                   @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                   @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Analytics received PaymentCompleted: txn={}, offset={}", event.getTransactionId(), offset);
        analyticsService.ingestPaymentEvent(event);
    }
}
