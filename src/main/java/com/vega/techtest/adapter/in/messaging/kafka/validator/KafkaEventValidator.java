package com.vega.techtest.adapter.in.messaging.kafka.validator;

import com.vega.techtest.adapter.in.messaging.kafka.dto.KafkaTransactionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Validates Kafka transaction event structure.
 * Checks event envelope and required fields in data payload.
 */
@Component
@Slf4j
public class KafkaEventValidator {

    /**
     * Validates the Kafka event structure.
     * Throws IllegalArgumentException if validation fails.
     */
    public void validate(KafkaTransactionEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        // Validate event envelope
        if (event.eventId() == null || event.eventId().isBlank()) {
            throw new IllegalArgumentException("Event ID is required");
        }

        if (event.eventType() == null || event.eventType().isBlank()) {
            throw new IllegalArgumentException("Event type is required");
        }

        if (event.data() == null || event.data().isEmpty()) {
            throw new IllegalArgumentException("Event data cannot be null or empty");
        }

        // Validate required fields in data payload
        Map<String, Object> data = event.data();

        validateRequiredField(data, "storeId");
        validateRequiredField(data, "tillId");
        validateRequiredField(data, "paymentMethod");
        validateRequiredField(data, "totalAmount");
        validateRequiredField(data, "timestamp");

        log.debug("Event validation passed for eventId: {}", event.eventId());
    }

    private void validateRequiredField(Map<String, Object> data, String fieldName) {
        if (!data.containsKey(fieldName) || data.get(fieldName) == null) {
            throw new IllegalArgumentException("Required field '" + fieldName + "' is missing in event data");
        }

        Object value = data.get(fieldName);
        if (value instanceof String && ((String) value).isBlank()) {
            throw new IllegalArgumentException("Required field '" + fieldName + "' cannot be blank");
        }
    }
}
