package com.vega.techtest.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record TransactionItemResponse(
        @JsonProperty("productName") String productName,
        @JsonProperty("productCode") String productCode,
        @JsonProperty("unitPrice") BigDecimal unitPrice,
        @JsonProperty("quantity") Integer quantity,
        @JsonProperty("totalPrice") BigDecimal totalPrice,
        @JsonProperty("category") String category
) {
    @JsonCreator
    public TransactionItemResponse {
    }
}
