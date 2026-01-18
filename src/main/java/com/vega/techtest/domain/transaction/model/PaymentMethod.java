package com.vega.techtest.domain.transaction.model;

import java.util.Locale;

public enum PaymentMethod {
    CASH,
    CARD;

    public static PaymentMethod fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Payment method is required");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Payment method is required");
        }
        switch (normalized) {
            case "cash":
                return CASH;
            case "card":
                return CARD;
            default:
                throw new IllegalArgumentException("Payment method must be 'cash' or 'card'");
        }
    }
}
