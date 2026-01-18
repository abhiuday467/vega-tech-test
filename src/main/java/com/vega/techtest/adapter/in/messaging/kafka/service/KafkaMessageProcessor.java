package com.vega.techtest.adapter.in.messaging.kafka.service;

import com.vega.techtest.adapter.in.messaging.kafka.dto.KafkaTransactionEvent;
import com.vega.techtest.adapter.in.messaging.kafka.exception.EventProcessingException;
import com.vega.techtest.adapter.in.messaging.kafka.mapper.KafkaEventMapper;
import com.vega.techtest.adapter.in.messaging.kafka.validator.KafkaEventValidator;
import com.vega.techtest.application.transaction.command.CreateTransactionCommand;
import com.vega.techtest.application.transaction.command.TransactionResult;
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

    public TransactionResult process(KafkaTransactionEvent event) {
        String eventId = event.eventId();

        try {
            log.info("Processing Kafka event: {}", eventId);

            validator.validate(event);

            CreateTransactionCommand command = mapper.toCommand(event);

            TransactionResult result = transactionService.processTransaction(command);

            log.info("Successfully processed event: {} -> transaction: {}", eventId, result.transactionId());

            return result;

        } catch (IllegalArgumentException e) {
            log.error("Validation failed for event: {}", eventId, e);
            throw e;

        } catch (Exception e) {
            log.error("Failed to process event: {}", eventId, e);
            throw new EventProcessingException(eventId, "Failed to process event", e);
        }
    }
}
