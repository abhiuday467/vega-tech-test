package com.vega.techtest.adapter.in.messaging.kafka.consumer;

import com.vega.techtest.adapter.in.messaging.kafka.dto.KafkaTransactionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for transaction events.
 * Phase 1: Basic message reception and logging.
 */
@Component
@Slf4j
public class TransactionKafkaConsumer {

    @KafkaListener(
            topics = "${kafka.topic.transactions}",
            groupId = "${kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            KafkaTransactionEvent event,
            Acknowledgment acknowledgment,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("=".repeat(80));
        log.info("Received Kafka Message:");
        log.info("Topic: {}, Partition: {}, Offset: {}", topic, partition, offset);
        log.info("Event ID: {}", event.eventId());
        log.info("Event Type: {}", event.eventType());
        log.info("Event Timestamp: {}", event.eventTimestamp());
        log.info("Source: {}", event.source());
        log.info("Version: {}", event.version());
        log.info("Data: {}", event.data());
        log.info("=".repeat(80));

        // Manually acknowledge the message
        if (acknowledgment != null) {
            acknowledgment.acknowledge();
            log.info("Message acknowledged successfully");
        }
    }
}
