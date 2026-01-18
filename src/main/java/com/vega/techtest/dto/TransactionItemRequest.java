package com.vega.techtest.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public class TransactionItemRequest {

    private final String productName;
    private final String productCode;
    private final BigDecimal unitPrice;
    private final Integer quantity;
    private final String category;

    @JsonCreator
    public TransactionItemRequest(
            @JsonProperty("productName") String productName,
            @JsonProperty("productCode") String productCode,
            @JsonProperty("unitPrice") BigDecimal unitPrice,
            @JsonProperty("quantity") Integer quantity,
            @JsonProperty("category") String category
    ) {
        this.productName = productName;
        this.productCode = productCode;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.category = category;
    }

    public TransactionItemRequest(String productName, String productCode,
                                  BigDecimal unitPrice, Integer quantity) {
        this(productName, productCode, unitPrice, quantity, null);
    }

    public String getProductName() {
        return productName;
    }

    public String getProductCode() {
        return productCode;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public String getCategory() {
        return category;
    }
}
