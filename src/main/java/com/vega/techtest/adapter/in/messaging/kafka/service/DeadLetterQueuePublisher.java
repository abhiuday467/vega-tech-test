package com.vega.techtest.adapter.in.messaging.kafka.service;

import com.vega.techtest.adapter.in.messaging.kafka.dto.KafkaTransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeadLetterQueuePublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.dlq}")
    private String dlqTopic;

    /**
     * Publishes a failed event to the DLQ with enriched metadata.
     *
     * @param event           The original Kafka event that failed
     * @param exception       The exception that caused the failure
     * @param originalTopic   The topic from which the event was consumed
     * @param partition       The partition from which the event was consumed
     * @param offset          The offset of the failed message
     */
    public void publishToDeadLetterQueue(
            KafkaTransactionEvent event,
            Exception exception,
            String originalTopic,
            int partition,
            long offset
    ) {
        try {
            Message<KafkaTransactionEvent> message = MessageBuilder
                    .withPayload(event)
                    .setHeader(KafkaHeaders.TOPIC, dlqTopic)
                    .setHeader(KafkaHeaders.KEY, event.eventId())
                    .setHeader("dlq-original-topic", originalTopic)
                    .setHeader("dlq-partition", partition)
                    .setHeader("dlq-offset", offset)
                    .setHeader("dlq-exception-type", exception.getClass().getName())
                    .setHeader("dlq-exception-message", exception.getMessage())
                    .setHeader("dlq-timestamp", Instant.now().toString())
                    .build();

            kafkaTemplate.send(message);

            log.warn("Event {} sent to DLQ. Original topic: {}, Partition: {}, Offset: {}, Exception: {}",
                    event.eventId(), originalTopic, partition, offset, exception.getClass().getSimpleName());

        } catch (Exception e) {
            log.error("Failed to send event {} to DLQ - acknowledging original message to prevent infinite loop. " +
                            "Original exception: {}, DLQ write exception: {}",
                    event.eventId(), exception.getMessage(), e.getMessage(), e);
        }
    }
}
