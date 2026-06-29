package com.swiftpay.ledger.consumer;

import com.swiftpay.ledger.dto.PaymentInitiatedEvent;
import com.swiftpay.ledger.dto.PaymentResultEvent;
import com.swiftpay.ledger.kafka.PaymentResultProducer;
import com.swiftpay.ledger.service.LedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentInitiatedConsumer {

    private final LedgerService ledgerService;
    private final PaymentResultProducer resultProducer;

    /**
     * Listens on payment.initiated topic and processes transfers.
     * RetryableTopic handles retries with backoff — if DB is down the event
     * will retry up to 5 times before going to DLT.
     */
    @RetryableTopic(
        attempts = "5",
        backoff = @Backoff(delay = 2000, multiplier = 2.0, maxDelay = 30000),
        topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
        dltTopicSuffix = ".dlt"
    )
    @KafkaListener(topics = "${swiftpay.kafka.topics.payment-initiated}",
                   groupId = "${spring.kafka.consumer.group-id}",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onPaymentInitiated(@Payload PaymentInitiatedEvent event,
                                   @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                   @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                   @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received PaymentInitiated: txn={}, topic={}, partition={}, offset={}",
                event.getTransactionId(), topic, partition, offset);

        PaymentResultEvent result = ledgerService.processPayment(event);
        resultProducer.publishResult(result);

        log.info("Payment processing complete: txn={}, status={}", 
                event.getTransactionId(), result.getStatus());
    }

    /**
     * Catches events that failed all retries.
     * For now just logging — ideally this should trigger an alert or manual review queue.
     * TODO: hook up alerting here
     */
    @KafkaListener(topics = "${swiftpay.kafka.topics.payment-initiated}.dlt",
                   groupId = "${spring.kafka.consumer.group-id}-dlt")
    public void onDeadLetter(@Payload PaymentInitiatedEvent event,
                              @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("DEAD LETTER: txn={} exhausted all retries, topic={}",
                event.getTransactionId(), topic);
        // TODO: hook up alerting here
    }
}
