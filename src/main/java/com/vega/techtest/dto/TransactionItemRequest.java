package com.vega.techtest.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record TransactionItemRequest(
        @JsonProperty("productName") String productName,
        @JsonProperty("productCode") String productCode,
        @JsonProperty("unitPrice") BigDecimal unitPrice,
        @JsonProperty("quantity") Integer quantity,
        @JsonProperty("category") String category
) {
    @JsonCreator
    public TransactionItemRequest {
    }

    public TransactionItemRequest(String productName, String productCode,
                                  BigDecimal unitPrice, Integer quantity) {
        this(productName, productCode, unitPrice, quantity, null);
    }
}
