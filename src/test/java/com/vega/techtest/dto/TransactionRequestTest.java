package com.vega.techtest.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZonedDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TransactionRequestTest {

    @Test
    @DisplayName("Should use timestamp, storeId, and tillId for equality")
    void equalsAndHashCode_usesCompositeKey() {
        ZonedDateTime timestamp = ZonedDateTime.parse("2024-01-01T10:15:30Z");

        TransactionRequest first = new TransactionRequest();
        first.setTimestamp(timestamp);
        first.setStoreId("STORE-1");
        first.setTillId("TILL-1");
        first.setCustomerId("CUST-1");

        TransactionRequest second = new TransactionRequest();
        second.setTimestamp(timestamp);
        second.setStoreId("STORE-1");
        second.setTillId("TILL-1");
        second.setCustomerId("CUST-2");

        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    @DisplayName("Should not be equal when composite key differs")
    void equalsAndHashCode_differsWhenKeyDiffers() {
        ZonedDateTime timestamp = ZonedDateTime.parse("2024-01-01T10:15:30Z");

        TransactionRequest first = new TransactionRequest();
        first.setTimestamp(timestamp);
        first.setStoreId("STORE-1");
        first.setTillId("TILL-1");

        TransactionRequest second = new TransactionRequest();
        second.setTimestamp(timestamp);
        second.setStoreId("STORE-2");
        second.setTillId("TILL-1");

        assertThat(first).isNotEqualTo(second);
    }
}
