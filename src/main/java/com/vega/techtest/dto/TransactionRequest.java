package com.vega.techtest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

@EqualsAndHashCode(of = {"timestamp", "storeId", "tillId"})
@Getter
@Setter
public class TransactionRequest {

    private String transactionId;
    private String customerId;

    @NotBlank(message = "Store ID is required")
    private String storeId;

    @NotBlank(message = "Till ID is required")
    private String tillId;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.01", message = "Total amount must be greater than zero")
    private BigDecimal totalAmount;

    private String currency = "GBP";

    @NotNull(message = "Transaction creation time is required")
    private ZonedDateTime timestamp;

    private List<TransactionItemRequest> items;

    public TransactionRequest() {
    }

    public TransactionRequest(String transactionId, String customerId, String storeId,
                              String tillId, String paymentMethod, BigDecimal totalAmount) {
        this.transactionId = transactionId;
        this.customerId = customerId;
        this.storeId = storeId;
        this.tillId = tillId;
        this.paymentMethod = paymentMethod;
        this.totalAmount = totalAmount;
        this.timestamp = ZonedDateTime.now();
    }
}
