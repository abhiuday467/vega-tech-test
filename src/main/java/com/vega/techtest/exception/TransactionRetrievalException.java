package com.vega.techtest.exception;

public class TransactionRetrievalException extends RuntimeException {

    public TransactionRetrievalException(String message) {
        super(message);
    }

    public TransactionRetrievalException(String message, Throwable cause) {
        super(message, cause);
    }
}
