package com.vega.techtest.adapter.in.messaging.kafka.mapper;

import com.vega.techtest.adapter.in.messaging.kafka.dto.KafkaTransactionEvent;
import com.vega.techtest.application.transaction.command.CreateTransactionCommand;
import com.vega.techtest.domain.transaction.model.TransactionItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Maps Kafka transaction events to domain commands.
 * Handles type conversions from Map<String, Object> to typed fields.
 */
@Component
@Slf4j
public class KafkaEventMapper {

    /**
     * Maps KafkaTransactionEvent to CreateTransactionCommand.
     */
    public CreateTransactionCommand toCommand(KafkaTransactionEvent event) {
        Map<String, Object> data = event.data();

        return new CreateTransactionCommand(
                extractString(data, "transactionId"),
                extractString(data, "customerId"),
                extractString(data, "storeId"),
                extractString(data, "tillId"),
                extractString(data, "paymentMethod"),
                extractBigDecimal(data, "totalAmount"),
                extractString(data, "currency"),
                extractInstant(data, "timestamp"),
                extractItems(data)
        );
    }

    private String extractString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }

    private BigDecimal extractBigDecimal(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }

        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }

        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }

        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Failed to convert '{}' to BigDecimal for key '{}'", value, key);
            return null;
        }
    }

    private Instant extractInstant(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }

        if (value instanceof Instant) {
            return (Instant) value;
        }

        try {
            return Instant.parse(value.toString());
        } catch (Exception e) {
            log.warn("Failed to parse timestamp '{}' for key '{}'", value, key);
            return null;
        }
    }

    private Integer extractInteger(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }

        if (value instanceof Integer) {
            return (Integer) value;
        }

        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Failed to convert '{}' to Integer for key '{}'", value, key);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<TransactionItem> extractItems(Map<String, Object> data) {
        Object itemsObj = data.get("items");
        if (itemsObj == null) {
            return new ArrayList<>();
        }

        if (!(itemsObj instanceof List)) {
            log.warn("Items field is not a list: {}", itemsObj.getClass());
            return new ArrayList<>();
        }

        List<TransactionItem> items = new ArrayList<>();
        List<Map<String, Object>> itemsList = (List<Map<String, Object>>) itemsObj;

        for (Map<String, Object> itemData : itemsList) {
            try {
                TransactionItem item = mapToTransactionItem(itemData);
                items.add(item);
            } catch (Exception e) {
                log.warn("Failed to map transaction item: {}", itemData, e);
                // Continue with other items
            }
        }

        return items;
    }

    private TransactionItem mapToTransactionItem(Map<String, Object> itemData) {
        return new TransactionItem(
                extractString(itemData, "productName"),
                extractString(itemData, "productCode"),
                extractBigDecimal(itemData, "unitPrice"),
                extractInteger(itemData, "quantity"),
                extractString(itemData, "category")
        );
    }
}
