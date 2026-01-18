package com.vega.techtest.validators;

import com.vega.techtest.dto.TransactionRequest;
import com.vega.techtest.exception.ReceiptTotalMismatchException;
import com.vega.techtest.service.command.CreateTransactionCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TransactionValidator {
    private static final Logger logger = LoggerFactory.getLogger(TransactionValidator.class);

    public void validateTransactionCommand(CreateTransactionCommand command) {
        validateCommandRequiredFields(command);
        validateCommandItems(command);

        if (command.items() != null && !command.items().isEmpty()) {
            BigDecimal calculatedTotal = command.items().stream()
                    .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (calculatedTotal.compareTo(command.totalAmount()) != 0) {
                logger.error("Calculated total ({}) doesn't match provided total ({})",
                        calculatedTotal, command.totalAmount());
                throw new ReceiptTotalMismatchException(
                        "Receipt total mismatch: calculated total does not match provided total",
                        calculatedTotal,
                        command.totalAmount()
                );
            }
        }
    }

    public void validateTransactionRequest(TransactionRequest request) {
        validateRequiredFields(request);
        validateItems(request);

        if (request.items() != null && !request.items().isEmpty()) {
            BigDecimal calculatedTotal = request.items().stream()
                    .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (calculatedTotal.compareTo(request.totalAmount()) != 0) {
                logger.error("Calculated total ({}) doesn't match provided total ({})",
                        calculatedTotal, request.totalAmount());
                throw new ReceiptTotalMismatchException(
                        "Receipt total mismatch: calculated total does not match provided total",
                        calculatedTotal,
                        request.totalAmount()
                );
            }
        }
    }

    private void validateRequiredFields(TransactionRequest request) {
        if (request.storeId() == null || request.storeId().trim().isEmpty()) {
            throw new IllegalArgumentException("Store ID is required");
        }
        if (request.tillId() == null || request.tillId().trim().isEmpty()) {
            throw new IllegalArgumentException("Till ID is required");
        }
        if (request.paymentMethod() == null || request.paymentMethod().trim().isEmpty()) {
            throw new IllegalArgumentException("Payment method is required");
        }
        if (request.totalAmount() == null) {
            throw new IllegalArgumentException("Total amount is required");
        }
        if (request.totalAmount().compareTo(new BigDecimal("0.01")) < 0) {
            throw new IllegalArgumentException("Total amount must be greater than zero");
        }
        if (request.timestamp() == null) {
            throw new IllegalArgumentException("Transaction creation time is required");
        }
    }

    private void validateItems(TransactionRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            return;
        }

        for (int i = 0; i < request.items().size(); i++) {
            var item = request.items().get(i);

            if (item == null) {
                throw new IllegalArgumentException("Item at index " + i + " cannot be null");
            }

            if (item.unitPrice() == null) {
                throw new IllegalArgumentException("Item at index " + i + ": Unit price is required");
            }

            if (item.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Item at index " + i + ": Unit price cannot be negative");
            }

            if (item.quantity() == null) {
                throw new IllegalArgumentException("Item at index " + i + ": Quantity is required");
            }

            if (item.quantity() <= 0) {
                throw new IllegalArgumentException("Item at index " + i + ": Quantity must be greater than zero");
            }
        }
    }

    private void validateCommandRequiredFields(CreateTransactionCommand command) {
        if (command.transactionId() != null && !command.transactionId().trim().isEmpty()) {
            validateTransactionIdFormat(command.transactionId());
        }
        if (command.storeId() == null || command.storeId().trim().isEmpty()) {
            throw new IllegalArgumentException("Store ID is required");
        }
        if (command.tillId() == null || command.tillId().trim().isEmpty()) {
            throw new IllegalArgumentException("Till ID is required");
        }
        if (command.paymentMethod() == null || command.paymentMethod().trim().isEmpty()) {
            throw new IllegalArgumentException("Payment method is required");
        }
        if (command.totalAmount() == null) {
            throw new IllegalArgumentException("Total amount is required");
        }
        if (command.totalAmount().compareTo(new BigDecimal("0.01")) < 0) {
            throw new IllegalArgumentException("Total amount must be greater than zero");
        }
        if (command.timestamp() == null) {
            throw new IllegalArgumentException("Transaction creation time is required");
        }
    }

    private void validateTransactionIdFormat(String transactionId) {
        String trimmedId = transactionId.trim();

        if (!trimmedId.startsWith("TXN-")) {
            throw new IllegalArgumentException("Transaction ID must start with 'TXN-'");
        }

        String suffix = trimmedId.substring(4);
        try {
            java.util.UUID.fromString(suffix);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Transaction ID must have a valid UUID after 'TXN-' prefix");
        }
    }

    private void validateCommandItems(CreateTransactionCommand command) {
        if (command.items() == null || command.items().isEmpty()) {
            return;
        }

        for (int i = 0; i < command.items().size(); i++) {
            var item = command.items().get(i);

            if (item == null) {
                throw new IllegalArgumentException("Item at index " + i + " cannot be null");
            }

            if (item.unitPrice() == null) {
                throw new IllegalArgumentException("Item at index " + i + ": Unit price is required");
            }

            if (item.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Item at index " + i + ": Unit price cannot be negative");
            }

            if (item.quantity() == null) {
                throw new IllegalArgumentException("Item at index " + i + ": Quantity is required");
            }

            if (item.quantity() <= 0) {
                throw new IllegalArgumentException("Item at index " + i + ": Quantity must be greater than zero");
            }
        }
    }

}
