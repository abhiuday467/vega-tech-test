package com.vega.techtest.validators;

import com.vega.techtest.dto.TransactionRequest;
import com.vega.techtest.exception.ReceiptTotalMismatchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TransactionValidator {
    private static final Logger logger = LoggerFactory.getLogger(TransactionValidator.class);

    public void validateTransactionRequest(TransactionRequest request) {
        validateRequiredFields(request);
        validateItems(request);

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            BigDecimal calculatedTotal = request.getItems().stream()
                    .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (calculatedTotal.compareTo(request.getTotalAmount()) != 0) {
                logger.error("Calculated total ({}) doesn't match provided total ({})",
                        calculatedTotal, request.getTotalAmount());
                throw new ReceiptTotalMismatchException(
                        "Receipt total mismatch: calculated total does not match provided total",
                        calculatedTotal,
                        request.getTotalAmount()
                );
            }
        }
    }

    private void validateRequiredFields(TransactionRequest request) {
        if (request.getStoreId() == null || request.getStoreId().trim().isEmpty()) {
            throw new IllegalArgumentException("Store ID is required");
        }
        if (request.getTillId() == null || request.getTillId().trim().isEmpty()) {
            throw new IllegalArgumentException("Till ID is required");
        }
        if (request.getPaymentMethod() == null || request.getPaymentMethod().trim().isEmpty()) {
            throw new IllegalArgumentException("Payment method is required");
        }
        if (request.getTotalAmount() == null) {
            throw new IllegalArgumentException("Total amount is required");
        }
        if (request.getTotalAmount().compareTo(new BigDecimal("0.01")) < 0) {
            throw new IllegalArgumentException("Total amount must be greater than zero");
        }
        if (request.getTimestamp() == null) {
            throw new IllegalArgumentException("Transaction creation time is required");
        }
    }

    private void validateItems(TransactionRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            return; // Items are optional
        }

        for (int i = 0; i < request.getItems().size(); i++) {
            var item = request.getItems().get(i);

            if (item == null) {
                throw new IllegalArgumentException("Item at index " + i + " cannot be null");
            }

            if (item.getUnitPrice() == null) {
                throw new IllegalArgumentException("Item at index " + i + ": Unit price is required");
            }

            if (item.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Item at index " + i + ": Unit price cannot be negative");
            }

            if (item.getQuantity() == null) {
                throw new IllegalArgumentException("Item at index " + i + ": Quantity is required");
            }

            if (item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Item at index " + i + ": Quantity must be greater than zero");
            }
        }
    }

}
