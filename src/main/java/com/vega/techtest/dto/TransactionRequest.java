package com.vega.techtest.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

public record TransactionRequest(
        @JsonProperty("transactionId") String transactionId,
        @JsonProperty("customerId") String customerId,
        @NotBlank(message = "Store ID is required")
        @JsonProperty("storeId") String storeId,
        @NotBlank(message = "Till ID is required")
        @JsonProperty("tillId") String tillId,
        @NotBlank(message = "Payment method is required")
        @JsonProperty("paymentMethod") String paymentMethod,
        @NotNull(message = "Total amount is required")
        @DecimalMin(value = "0.01", message = "Total amount must be greater than zero")
        @JsonProperty("totalAmount") BigDecimal totalAmount,
        @JsonProperty("currency") String currency,
        @NotNull(message = "Transaction creation time is required")
        @JsonProperty("timestamp") ZonedDateTime timestamp,
        @JsonProperty("items") List<TransactionItemRequest> items
) {
    @JsonCreator
    public TransactionRequest {
        if (currency == null) {
            currency = "GBP";
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TransactionRequest that)) {
            return false;
        }
        return Objects.equals(timestamp, that.timestamp)
                && Objects.equals(storeId, that.storeId)
                && Objects.equals(tillId, that.tillId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, storeId, tillId);
    }
}
