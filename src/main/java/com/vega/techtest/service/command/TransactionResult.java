package com.vega.techtest.service.command;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

public record TransactionResult(
    String transactionId,
    String customerId,
    String storeId,
    String tillId,
    String paymentMethod,
    BigDecimal totalAmount,
    String currency,
    ZonedDateTime transactionTimestamp,
    ZonedDateTime createdAt,
    String status,
    List<TransactionItemResult> items
) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionResult that = (TransactionResult) o;
        return Objects.equals(transactionTimestamp, that.transactionTimestamp) &&
               Objects.equals(storeId, that.storeId) &&
               Objects.equals(tillId, that.tillId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionTimestamp, storeId, tillId);
    }
}
