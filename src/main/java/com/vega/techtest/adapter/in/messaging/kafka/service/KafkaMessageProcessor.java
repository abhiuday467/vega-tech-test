package com.vega.techtest.adapter.in.messaging.kafka.service;

import com.vega.techtest.adapter.in.messaging.kafka.dto.KafkaTransactionEvent;
import com.vega.techtest.adapter.in.messaging.kafka.exception.DuplicateEventException;
import com.vega.techtest.adapter.in.messaging.kafka.exception.EventProcessingException;
import com.vega.techtest.adapter.in.messaging.kafka.mapper.KafkaEventMapper;
import com.vega.techtest.adapter.in.messaging.kafka.validator.KafkaEventValidator;
import com.vega.techtest.application.transaction.command.CreateTransactionCommand;
import com.vega.techtest.application.transaction.command.TransactionResult;
import com.vega.techtest.domain.transaction.exception.ReceiptTotalMismatchException;
import com.vega.techtest.domain.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaMessageProcessor {

    private final KafkaEventValidator validator;
    private final KafkaEventMapper mapper;
    private final TransactionService transactionService;
    private final EventIdempotencyService idempotencyService;

    public TransactionResult process(KafkaTransactionEvent event) {
        String eventId = event.eventId();
        String eventType = event.eventType();

        // Check for duplicate event (idempotency)
        boolean lockAcquired = idempotencyService.tryAcquireProcessingLock(eventId, eventType);
        if (!lockAcquired) {
            log.warn("Duplicate event detected and skipped: {}", eventId);
            throw new DuplicateEventException(eventId);
        }

        try {
            log.info("Processing Kafka event: {}", eventId);

            validator.validate(event);

            CreateTransactionCommand command = mapper.toCommand(event);

            TransactionResult result = transactionService.processTransaction(command);

            // Mark as completed on success
            idempotencyService.markAsCompleted(eventId);

            log.info("Successfully processed event: {} -> transaction: {}", eventId, result.transactionId());

            return result;

        } catch (IllegalArgumentException e) {
            // Mark as failed for validation errors (non-retriable)
            idempotencyService.markAsFailed(eventId, "Validation error: " + e.getMessage());
            log.error("Validation failed for event: {}", eventId, e);
            throw e;

        } catch (IllegalStateException e) {
            // Mark as failed for invalid state errors (non-retriable)
            idempotencyService.markAsFailed(eventId, "Invalid state: " + e.getMessage());
            log.error("Invalid state for event: {}", eventId, e);
            throw e;

        } catch (ReceiptTotalMismatchException e) {
            // Mark as failed for business rule violations (non-retriable)
            idempotencyService.markAsFailed(eventId,
                String.format("Receipt total mismatch: calculated=%s, provided=%s",
                    e.getCalculatedTotal(), e.getProvidedTotal()));
            log.error("Receipt total mismatch for event: {}", eventId, e);
            throw e;

        } catch (Exception e) {
            // Mark as failed for processing errors (retriable - will be retried by Kafka)
            idempotencyService.markAsFailed(eventId, "Processing error: " + e.getMessage());
            log.error("Failed to process event: {}", eventId, e);
            throw new EventProcessingException(eventId, "Failed to process event", e);
        }
    }
}
