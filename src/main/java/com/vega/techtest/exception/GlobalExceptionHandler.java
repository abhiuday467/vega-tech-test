package com.vega.techtest.exception;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Counter transactionErrorCounter;

    @Autowired
    public GlobalExceptionHandler(MeterRegistry meterRegistry) {
        this.transactionErrorCounter = Counter.builder("transaction_errors_total")
                .description("Total number of transaction processing errors")
                .register(meterRegistry);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing
                ));

        logger.warn("Validation failed for fields: {}", fieldErrors);
        transactionErrorCounter.increment();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "Invalid transaction data");
        response.put("errors", fieldErrors);

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        logger.warn("Invalid request: {}", ex.getMessage());
        transactionErrorCounter.increment();

        return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Invalid transaction data",
                "error", ex.getMessage()
        ));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Void> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        logger.debug("Resource not found: {}", ex.getMessage());
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        logger.warn("Invalid request parameter: {}", ex.getMessage());
        transactionErrorCounter.increment();

        return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Invalid request parameter",
                "error", "Invalid parameter value"
        ));
    }

    @ExceptionHandler(TransactionProcessingException.class)
    public ResponseEntity<Map<String, Object>> handleTransactionProcessingException(
            TransactionProcessingException ex, WebRequest request) {
        logger.error("Transaction processing error: {}", ex.getMessage(), ex);
        transactionErrorCounter.increment();

        return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", ex.getMessage(),
                "error", "Internal server error"
        ));
    }

    @ExceptionHandler(TransactionRetrievalException.class)
    public ResponseEntity<Map<String, Object>> handleTransactionRetrievalException(
            TransactionRetrievalException ex, WebRequest request) {
        logger.error("Transaction retrieval error: {}", ex.getMessage(), ex);
        transactionErrorCounter.increment();

        return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", ex.getMessage(),
                "error", "Internal server error"
        ));
    }

    @ExceptionHandler(StatisticsCalculationException.class)
    public ResponseEntity<Map<String, Object>> handleStatisticsCalculationException(
            StatisticsCalculationException ex, WebRequest request) {
        logger.error("Statistics calculation error: {}", ex.getMessage(), ex);
        transactionErrorCounter.increment();

        return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", ex.getMessage(),
                "error", "Internal server error"
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, WebRequest request) {
        logger.error("Internal server error occurred", ex);
        transactionErrorCounter.increment();

        return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", "Failed to process request",
                "error", "Internal server error"
        ));
    }
}
