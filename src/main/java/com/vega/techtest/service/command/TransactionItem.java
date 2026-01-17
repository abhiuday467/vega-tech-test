package com.vega.techtest.service.command;

import java.math.BigDecimal;

public record TransactionItem(
    String productName,
    String productCode,
    BigDecimal unitPrice,
    Integer quantity,
    String category
) {
}
