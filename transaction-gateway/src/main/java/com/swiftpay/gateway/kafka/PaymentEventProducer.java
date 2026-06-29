package com.swiftpay.gateway.kafka;

import com.swiftpay.gateway.dto.PaymentInitiatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${swiftpay.kafka.topics.payment-initiated}")
    private String paymentInitiatedTopic;

    public void publishPaymentInitiated(PaymentInitiatedEvent event) {
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(paymentInitiatedTopic, event.getTransactionId(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish PaymentInitiated event for txn={}: {}",
                        event.getTransactionId(), ex.getMessage(), ex);
            } else {
                log.info("PaymentInitiated event published: txn={}, partition={}, offset={}",
                        event.getTransactionId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
