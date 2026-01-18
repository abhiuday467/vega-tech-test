package com.vega.techtest.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.beans.ConstructorProperties;

public class TransactionResponse {

    private final String transactionId;
    private final String customerId;
    private final String storeId;
    private final String tillId;
    private final String paymentMethod;
    private final BigDecimal totalAmount;
    private final String currency;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private final ZonedDateTime transactionTimestamp;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private final ZonedDateTime createdAt;

    private final String status;
    private final List<TransactionItemResponse> items;

    @JsonCreator
    @ConstructorProperties({
            "transactionId",
            "customerId",
            "storeId",
            "tillId",
            "paymentMethod",
            "totalAmount",
            "currency",
            "transactionTimestamp",
            "createdAt",
            "status",
            "items"
    })
    public TransactionResponse(
            @JsonProperty("transactionId") String transactionId,
            @JsonProperty("customerId") String customerId,
            @JsonProperty("storeId") String storeId,
            @JsonProperty("tillId") String tillId,
            @JsonProperty("paymentMethod") String paymentMethod,
            @JsonProperty("totalAmount") BigDecimal totalAmount,
            @JsonProperty("currency") String currency,
            @JsonProperty("transactionTimestamp") ZonedDateTime transactionTimestamp,
            @JsonProperty("createdAt") ZonedDateTime createdAt,
            @JsonProperty("status") String status,
            @JsonProperty("items") List<TransactionItemResponse> items
    ) {
        this.transactionId = transactionId;
        this.customerId = customerId;
        this.storeId = storeId;
        this.tillId = tillId;
        this.paymentMethod = paymentMethod;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.transactionTimestamp = transactionTimestamp;
        this.createdAt = createdAt == null ? ZonedDateTime.now() : createdAt;
        this.status = status;
        this.items = items;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getStoreId() {
        return storeId;
    }

    public String getTillId() {
        return tillId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public ZonedDateTime getTransactionTimestamp() {
        return transactionTimestamp;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public String getStatus() {
        return status;
    }

    public List<TransactionItemResponse> getItems() {
        return items;
    }
}
