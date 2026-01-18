package com.vega.techtest.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Kafka transaction event message format.
 * The data field is a generic Map to allow candidates to parse it themselves.
 */
public record KafkaTransactionEvent(
        @JsonProperty("eventId") String eventId,
        @JsonProperty("eventType") String eventType,
        @JsonProperty("eventTimestamp") String eventTimestamp,
        @JsonProperty("source") String source,
        @JsonProperty("version") String version,
        @JsonProperty("data") Map<String, Object> data
) {
    @JsonCreator
    public KafkaTransactionEvent {
    }
}
