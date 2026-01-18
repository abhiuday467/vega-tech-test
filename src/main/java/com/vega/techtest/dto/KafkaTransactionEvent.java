package com.vega.techtest.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Kafka transaction event message format.
 * The data field is a generic Map to allow candidates to parse it themselves.
 */
public class KafkaTransactionEvent {

    private final String eventId;
    private final String eventType;
    private final String eventTimestamp;
    private final String source;
    private final String version;
    private final Map<String, Object> data;

    @JsonCreator
    public KafkaTransactionEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("eventTimestamp") String eventTimestamp,
            @JsonProperty("source") String source,
            @JsonProperty("version") String version,
            @JsonProperty("data") Map<String, Object> data
    ) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.eventTimestamp = eventTimestamp;
        this.source = source;
        this.version = version;
        this.data = data;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getEventTimestamp() {
        return eventTimestamp;
    }

    public String getSource() {
        return source;
    }

    public String getVersion() {
        return version;
    }

    public Map<String, Object> getData() {
        return data;
    }

    @Override
    public String toString() {
        return "KafkaTransactionEvent{" +
                "eventId='" + eventId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", eventTimestamp='" + eventTimestamp + '\'' +
                ", source='" + source + '\'' +
                ", version='" + version + '\'' +
                ", data=" + data +
                '}';
    }
}
