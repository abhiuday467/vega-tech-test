package com.vega.techtest.domain.transaction.model;

import java.math.BigDecimal;

public record TransactionItem(
    String productName,
    String productCode,
    BigDecimal unitPrice,
    Integer quantity,
    String category
) {
}
