package com.swiftpay.ledger.kafka;

import com.swiftpay.ledger.dto.PaymentResultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentResultProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${swiftpay.kafka.topics.payment-completed}")
    private String completedTopic;

    @Value("${swiftpay.kafka.topics.payment-failed}")
    private String failedTopic;

    public void publishResult(PaymentResultEvent event) {
        String topic = "COMPLETED".equals(event.getStatus()) ? completedTopic : failedTopic;

        kafkaTemplate.send(topic, event.getTransactionId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish {} event for txn={}: {}",
                                event.getStatus(), event.getTransactionId(), ex.getMessage());
                    } else {
                        log.info("Published {} event: txn={}", event.getStatus(), event.getTransactionId());
                    }
                });
    }
}
