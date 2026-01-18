package com.vega.techtest.application.transaction.command;

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
