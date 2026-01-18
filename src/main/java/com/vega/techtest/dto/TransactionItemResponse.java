package com.vega.techtest.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.beans.ConstructorProperties;

public class TransactionItemResponse {

    private final String productName;
    private final String productCode;
    private final BigDecimal unitPrice;
    private final Integer quantity;
    private final BigDecimal totalPrice;
    private final String category;

    @JsonCreator
    @ConstructorProperties({
            "productName",
            "productCode",
            "unitPrice",
            "quantity",
            "totalPrice",
            "category"
    })
    public TransactionItemResponse(
            @JsonProperty("productName") String productName,
            @JsonProperty("productCode") String productCode,
            @JsonProperty("unitPrice") BigDecimal unitPrice,
            @JsonProperty("quantity") Integer quantity,
            @JsonProperty("totalPrice") BigDecimal totalPrice,
            @JsonProperty("category") String category
    ) {
        this.productName = productName;
        this.productCode = productCode;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.category = category;
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

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public String getCategory() {
        return category;
    }
}
