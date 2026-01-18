package com.vega.techtest.service.command;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record CreateTransactionCommand(
    String transactionId,
    String customerId,
    String storeId,
    String tillId,
    String paymentMethod,
    BigDecimal totalAmount,
    String currency,
    Instant timestamp,
    List<TransactionItem> items
) {
    public CreateTransactionCommand {
        if (currency == null || currency.isEmpty()) {
            currency = "GBP";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreateTransactionCommand that = (CreateTransactionCommand) o;
        return Objects.equals(timestamp, that.timestamp) &&
               Objects.equals(storeId, that.storeId) &&
               Objects.equals(tillId, that.tillId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, storeId, tillId);
    }
}
