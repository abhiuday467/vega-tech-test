package com.vega.techtest.adapter.in.rest.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZonedDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TransactionRequestTest {

    @Test
    @DisplayName("Should use timestamp, storeId, and tillId for equality")
    void equalsAndHashCode_usesCompositeKey() {
        ZonedDateTime timestamp = ZonedDateTime.parse("2024-01-01T10:15:30Z");

        TransactionRequest first = new TransactionRequest(
                null,
                "CUST-1",
                "STORE-1",
                "TILL-1",
                "card",
                null,
                "GBP",
                timestamp,
                null
        );

        TransactionRequest second = new TransactionRequest(
                null,
                "CUST-2",
                "STORE-1",
                "TILL-1",
                "cash",
                null,
                "GBP",
                timestamp,
                null
        );

        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    @DisplayName("Should not be equal when composite key differs")
    void equalsAndHashCode_differsWhenKeyDiffers() {
        ZonedDateTime timestamp = ZonedDateTime.parse("2024-01-01T10:15:30Z");

        TransactionRequest first = new TransactionRequest(
                null,
                null,
                "STORE-1",
                "TILL-1",
                "card",
                null,
                "GBP",
                timestamp,
                null
        );

        TransactionRequest second = new TransactionRequest(
                null,
                null,
                "STORE-2",
                "TILL-1",
                "card",
                null,
                "GBP",
                timestamp,
                null
        );

        assertThat(first).isNotEqualTo(second);
    }
}
