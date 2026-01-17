package com.vega.techtest.service.command;

import java.math.BigDecimal;

public record TransactionItemResult(
    String productName,
    String productCode,
    BigDecimal unitPrice,
    Integer quantity,
    BigDecimal totalPrice,
    String category
) {
}
