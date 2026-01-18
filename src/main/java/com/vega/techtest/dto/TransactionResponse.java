package com.vega.techtest.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

public record TransactionResponse(
        @JsonProperty("transactionId") String transactionId,
        @JsonProperty("customerId") String customerId,
        @JsonProperty("storeId") String storeId,
        @JsonProperty("tillId") String tillId,
        @JsonProperty("paymentMethod") String paymentMethod,
        @JsonProperty("totalAmount") BigDecimal totalAmount,
        @JsonProperty("currency") String currency,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        @JsonProperty("transactionTimestamp") ZonedDateTime transactionTimestamp,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        @JsonProperty("createdAt") ZonedDateTime createdAt,
        @JsonProperty("status") String status,
        @JsonProperty("items") List<TransactionItemResponse> items
) {
    @JsonCreator
    public TransactionResponse {
    }
}
