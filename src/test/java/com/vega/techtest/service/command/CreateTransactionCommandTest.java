package com.vega.techtest.service.command;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CreateTransactionCommandTest {

    @Test
    @DisplayName("Should create command with all fields")
    void shouldCreateCommandWithAllFields() {
        ZonedDateTime timestamp = ZonedDateTime.now();
        List<TransactionItem> items = new ArrayList<>();

        CreateTransactionCommand command = new CreateTransactionCommand(
            "TXN-123",
            "CUST-1",
            "STORE-1",
            "TILL-1",
            "card",
            new BigDecimal("100.00"),
            "GBP",
            timestamp,
            items
        );

        assertThat(command.transactionId()).isEqualTo("TXN-123");
        assertThat(command.customerId()).isEqualTo("CUST-1");
        assertThat(command.storeId()).isEqualTo("STORE-1");
        assertThat(command.tillId()).isEqualTo("TILL-1");
        assertThat(command.paymentMethod()).isEqualTo("card");
        assertThat(command.totalAmount()).isEqualByComparingTo("100.00");
        assertThat(command.currency()).isEqualTo("GBP");
        assertThat(command.timestamp()).isEqualTo(timestamp);
        assertThat(command.items()).isEqualTo(items);
    }

    @Test
    @DisplayName("Should apply default currency when null")
    void shouldApplyDefaultCurrency() {
        ZonedDateTime timestamp = ZonedDateTime.now();

        CreateTransactionCommand command = new CreateTransactionCommand(
            "TXN-123",
            "CUST-1",
            "STORE-1",
            "TILL-1",
            "card",
            new BigDecimal("100.00"),
            null,
            timestamp,
            null
        );

        assertThat(command.currency()).isEqualTo("GBP");
    }

    @Test
    @DisplayName("Should apply default currency when empty")
    void shouldApplyDefaultCurrencyWhenEmpty() {
        ZonedDateTime timestamp = ZonedDateTime.now();

        CreateTransactionCommand command = new CreateTransactionCommand(
            "TXN-123",
            "CUST-1",
            "STORE-1",
            "TILL-1",
            "card",
            new BigDecimal("100.00"),
            "",
            timestamp,
            null
        );

        assertThat(command.currency()).isEqualTo("GBP");
    }

    @Test
    @DisplayName("Should use custom equals based on natural key (timestamp, storeId, tillId)")
    void shouldUseNaturalKeyForEquals() {
        ZonedDateTime timestamp = ZonedDateTime.now();

        CreateTransactionCommand command1 = new CreateTransactionCommand(
            "TXN-123",
            "CUST-1",
            "STORE-1",
            "TILL-1",
            "card",
            new BigDecimal("100.00"),
            "GBP",
            timestamp,
            null
        );

        CreateTransactionCommand command2 = new CreateTransactionCommand(
            "TXN-999",
            "CUST-2",
            "STORE-1",
            "TILL-1",
            "cash",
            new BigDecimal("200.00"),
            "USD",
            timestamp,
            new ArrayList<>()
        );

        assertThat(command1).isEqualTo(command2);
    }

    @Test
    @DisplayName("Should not be equal when natural key differs")
    void shouldNotBeEqualWhenNaturalKeyDiffers() {
        ZonedDateTime timestamp1 = ZonedDateTime.now();
        ZonedDateTime timestamp2 = timestamp1.plusMinutes(1);

        CreateTransactionCommand command1 = new CreateTransactionCommand(
            "TXN-123",
            "CUST-1",
            "STORE-1",
            "TILL-1",
            "card",
            new BigDecimal("100.00"),
            "GBP",
            timestamp1,
            null
        );

        CreateTransactionCommand command2 = new CreateTransactionCommand(
            "TXN-123",
            "CUST-1",
            "STORE-1",
            "TILL-1",
            "card",
            new BigDecimal("100.00"),
            "GBP",
            timestamp2,
            null
        );

        assertThat(command1).isNotEqualTo(command2);
    }

    @Test
    @DisplayName("Should use custom hashCode based on natural key")
    void shouldUseNaturalKeyForHashCode() {
        ZonedDateTime timestamp = ZonedDateTime.now();

        CreateTransactionCommand command1 = new CreateTransactionCommand(
            "TXN-123",
            "CUST-1",
            "STORE-1",
            "TILL-1",
            "card",
            new BigDecimal("100.00"),
            "GBP",
            timestamp,
            null
        );

        CreateTransactionCommand command2 = new CreateTransactionCommand(
            "TXN-999",
            "CUST-2",
            "STORE-1",
            "TILL-1",
            "cash",
            new BigDecimal("200.00"),
            "USD",
            timestamp,
            new ArrayList<>()
        );

        assertThat(command1.hashCode()).isEqualTo(command2.hashCode());
    }

    @Test
    @DisplayName("Should allow null transactionId")
    void shouldAllowNullTransactionId() {
        ZonedDateTime timestamp = ZonedDateTime.now();

        CreateTransactionCommand command = new CreateTransactionCommand(
            null,
            "CUST-1",
            "STORE-1",
            "TILL-1",
            "card",
            new BigDecimal("100.00"),
            "GBP",
            timestamp,
            null
        );

        assertThat(command.transactionId()).isNull();
    }
}
