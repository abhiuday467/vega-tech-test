package com.vega.techtest.adapter.in.messaging.kafka.exception;

public class DuplicateEventException extends RuntimeException {

    private final String eventId;

    public DuplicateEventException(String eventId) {
        super(String.format("Event with ID '%s' has already been processed", eventId));
        this.eventId = eventId;
    }

    public DuplicateEventException(String eventId, String message) {
        super(message);
        this.eventId = eventId;
    }

    public String getEventId() {
        return eventId;
    }
}
