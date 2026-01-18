package com.vega.techtest.adapter.in.messaging.kafka.consumer;

import com.vega.techtest.adapter.in.messaging.kafka.dto.KafkaTransactionEvent;
import com.vega.techtest.adapter.in.messaging.kafka.exception.DuplicateEventException;
import com.vega.techtest.adapter.in.messaging.kafka.service.DeadLetterQueuePublisher;
import com.vega.techtest.adapter.in.messaging.kafka.service.KafkaMessageProcessor;
import com.vega.techtest.application.transaction.command.TransactionResult;
import com.vega.techtest.domain.transaction.exception.ReceiptTotalMismatchException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class TransactionKafkaConsumer {

    private final KafkaMessageProcessor messageProcessor;
    private final DeadLetterQueuePublisher deadLetterQueuePublisher;

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
        log.info("Received Kafka message - Topic: {}, Partition: {}, Offset: {}, EventID: {}",
                topic, partition, offset, event.eventId());

        try {
            TransactionResult result = messageProcessor.process(event);

            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

            log.info("Successfully processed and acknowledged event: {} -> transaction: {}",
                    event.eventId(), result.transactionId());

        } catch (DuplicateEventException e) {
            log.info("Duplicate event detected for event: {} - Acknowledging without processing", event.eventId());

            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

        } catch (IllegalArgumentException e) {
            log.error("Validation error for event: {} - Sent to DLQ. Reason: {}",
                    event.eventId(), e.getMessage());
            deadLetterQueuePublisher.publishToDeadLetterQueue(event, e, topic, partition, offset);

            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

        } catch (IllegalStateException e) {
            log.error("Invalid state for event: {} - Sent to DLQ. Reason: {}",
                    event.eventId(), e.getMessage());
            deadLetterQueuePublisher.publishToDeadLetterQueue(event, e, topic, partition, offset);

            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

        } catch (ReceiptTotalMismatchException e) {
            log.error("Receipt total mismatch for event: {} - Sent to DLQ. Calculated: {}, Provided: {}",
                    event.eventId(), e.getCalculatedTotal(), e.getProvidedTotal());
            deadLetterQueuePublisher.publishToDeadLetterQueue(event, e, topic, partition, offset);

            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

        } catch (Exception e) {
            log.error("Processing error for event: {} - Message will be retried", event.eventId(), e);
            throw e;
        }
    }
}
