package com.vega.techtest.domain.transaction.exception;

public class StatisticsCalculationException extends RuntimeException {

    public StatisticsCalculationException(String message) {
        super(message);
    }

    public StatisticsCalculationException(String message, Throwable cause) {
        super(message, cause);
    }
}
