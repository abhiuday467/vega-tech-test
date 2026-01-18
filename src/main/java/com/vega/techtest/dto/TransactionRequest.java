package com.vega.techtest.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

@EqualsAndHashCode(of = {"timestamp", "storeId", "tillId"})
@Getter
public class TransactionRequest {

    private final String transactionId;
    private final String customerId;

    @NotBlank(message = "Store ID is required")
    private final String storeId;

    @NotBlank(message = "Till ID is required")
    private final String tillId;

    @NotBlank(message = "Payment method is required")
    private final String paymentMethod;

    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.01", message = "Total amount must be greater than zero")
    private final BigDecimal totalAmount;

    private final String currency;

    @NotNull(message = "Transaction creation time is required")
    private final ZonedDateTime timestamp;

    private final List<TransactionItemRequest> items;

    @JsonCreator
    public TransactionRequest(
            @JsonProperty("transactionId") String transactionId,
            @JsonProperty("customerId") String customerId,
            @JsonProperty("storeId") String storeId,
            @JsonProperty("tillId") String tillId,
            @JsonProperty("paymentMethod") String paymentMethod,
            @JsonProperty("totalAmount") BigDecimal totalAmount,
            @JsonProperty("currency") String currency,
            @JsonProperty("timestamp") ZonedDateTime timestamp,
            @JsonProperty("items") List<TransactionItemRequest> items
    ) {
        this.transactionId = transactionId;
        this.customerId = customerId;
        this.storeId = storeId;
        this.tillId = tillId;
        this.paymentMethod = paymentMethod;
        this.totalAmount = totalAmount;
        this.currency = currency == null ? "GBP" : currency;
        this.timestamp = timestamp;
        this.items = items;
    }
}
