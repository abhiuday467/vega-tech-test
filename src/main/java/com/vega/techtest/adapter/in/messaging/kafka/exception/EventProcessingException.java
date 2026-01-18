package com.vega.techtest.adapter.in.messaging.kafka.exception;

public class EventProcessingException extends RuntimeException {

    private final String eventId;

    public EventProcessingException(String eventId, String message) {
        super(message);
        this.eventId = eventId;
    }

    public EventProcessingException(String eventId, String message, Throwable cause) {
        super(message, cause);
        this.eventId = eventId;
    }

    public String getEventId() {
        return eventId;
    }
}
