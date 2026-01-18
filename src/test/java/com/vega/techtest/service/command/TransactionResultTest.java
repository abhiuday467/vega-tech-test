package com.vega.techtest.service.command;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionResultTest {

    @Test
    @DisplayName("Should create result with all fields")
    void shouldCreateResultWithAllFields() {
        Instant transactionTimestamp = Instant.now();
        Instant createdAt = Instant.now();
        List<TransactionItemResult> items = new ArrayList<>();

        TransactionResult result = new TransactionResult(
            "TXN-123",
            "CUST-1",
            "STORE-1",
            "TILL-1",
            "card",
            new BigDecimal("100.00"),
            "GBP",
            transactionTimestamp,
            createdAt,
            "COMPLETED",
            items
        );

        assertThat(result.transactionId()).isEqualTo("TXN-123");
        assertThat(result.customerId()).isEqualTo("CUST-1");
        assertThat(result.storeId()).isEqualTo("STORE-1");
        assertThat(result.tillId()).isEqualTo("TILL-1");
        assertThat(result.paymentMethod()).isEqualTo("card");
        assertThat(result.totalAmount()).isEqualByComparingTo("100.00");
        assertThat(result.currency()).isEqualTo("GBP");
        assertThat(result.transactionTimestamp()).isEqualTo(transactionTimestamp);
        assertThat(result.createdAt()).isEqualTo(createdAt);
        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.items()).isEqualTo(items);
    }

    @Test
    @DisplayName("Should use custom equals based on natural key (transactionTimestamp, storeId, tillId)")
    void shouldUseNaturalKeyForEquals() {
        Instant transactionTimestamp = Instant.now();
        Instant createdAt1 = Instant.now();
        Instant createdAt2 = createdAt1.plusSeconds(300);

        TransactionResult result1 = new TransactionResult(
            "TXN-123",
            "CUST-1",
            "STORE-1",
            "TILL-1",
            "card",
            new BigDecimal("100.00"),
            "GBP",
            transactionTimestamp,
            createdAt1,
            "COMPLETED",
            null
        );

        TransactionResult result2 = new TransactionResult(
            "TXN-999",
            "CUST-2",
            "STORE-1",
            "TILL-1",
            "cash",
            new BigDecimal("200.00"),
            "USD",
            transactionTimestamp,
            createdAt2,
            "PENDING",
            new ArrayList<>()
        );

        assertThat(result1).isEqualTo(result2);
    }

    @Test
    @DisplayName("Should not be equal when natural key differs")
    void shouldNotBeEqualWhenNaturalKeyDiffers() {
        Instant transactionTimestamp1 = Instant.now();
        Instant transactionTimestamp2 = transactionTimestamp1.plusSeconds(60);
        Instant createdAt = Instant.now();

        TransactionResult result1 = new TransactionResult(
            "TXN-123",
            "CUST-1",
            "STORE-1",
            "TILL-1",
            "card",
            new BigDecimal("100.00"),
            "GBP",
            transactionTimestamp1,
            createdAt,
            "COMPLETED",
            null
        );

        TransactionResult result2 = new TransactionResult(
            "TXN-123",
            "CUST-1",
            "STORE-1",
            "TILL-1",
            "card",
            new BigDecimal("100.00"),
            "GBP",
            transactionTimestamp2,
            createdAt,
            "COMPLETED",
            null
        );

        assertThat(result1).isNotEqualTo(result2);
    }

    @Test
    @DisplayName("Should use custom hashCode based on natural key")
    void shouldUseNaturalKeyForHashCode() {
        Instant transactionTimestamp = Instant.now();
        Instant createdAt1 = Instant.now();
        Instant createdAt2 = createdAt1.plusSeconds(300);

        TransactionResult result1 = new TransactionResult(
            "TXN-123",
            "CUST-1",
            "STORE-1",
            "TILL-1",
            "card",
            new BigDecimal("100.00"),
            "GBP",
            transactionTimestamp,
            createdAt1,
            "COMPLETED",
            null
        );

        TransactionResult result2 = new TransactionResult(
            "TXN-999",
            "CUST-2",
            "STORE-1",
            "TILL-1",
            "cash",
            new BigDecimal("200.00"),
            "USD",
            transactionTimestamp,
            createdAt2,
            "PENDING",
            new ArrayList<>()
        );

        assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
    }
}
