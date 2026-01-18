package com.vega.techtest.domain.transaction.validator;

import com.vega.techtest.adapter.in.rest.dto.TransactionItemRequest;
import com.vega.techtest.adapter.in.rest.dto.TransactionRequest;
import com.vega.techtest.domain.transaction.exception.ReceiptTotalMismatchException;
import com.vega.techtest.application.transaction.command.CreateTransactionCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class TransactionValidatorTest {

    private TransactionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TransactionValidator();
    }

    @Test
    @DisplayName("Should pass validation for valid request")
    void validateTransactionRequest_validRequest() {
        TransactionRequest request = createValidRequest();

        assertDoesNotThrow(() -> validator.validateTransactionRequest(request));
    }

    @Test
    @DisplayName("Should throw exception when storeId is null")
    void validateTransactionRequest_nullStoreId() {
        TransactionRequest request = buildRequest(
                null,
                "TILL-001",
                "card",
                new BigDecimal("10.00"),
                ZonedDateTime.now(),
                null
        );

        assertThatThrownBy(() -> validator.validateTransactionRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Store ID is required");
    }

    @Test
    @DisplayName("Should throw exception when storeId is blank")
    void validateTransactionRequest_blankStoreId() {
        TransactionRequest request = buildRequest(
                "   ",
                "TILL-001",
                "card",
                new BigDecimal("10.00"),
                ZonedDateTime.now(),
                null
        );

        assertThatThrownBy(() -> validator.validateTransactionRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Store ID is required");
    }

    @Test
    @DisplayName("Should throw exception when tillId is null")
    void validateTransactionRequest_nullTillId() {
        TransactionRequest request = buildRequest(
                "STORE-001",
                null,
                "card",
                new BigDecimal("10.00"),
                ZonedDateTime.now(),
                null
        );

        assertThatThrownBy(() -> validator.validateTransactionRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Till ID is required");
    }

    @Test
    @DisplayName("Should throw exception when tillId is blank")
    void validateTransactionRequest_blankTillId() {
        TransactionRequest request = buildRequest(
                "STORE-001",
                "",
                "card",
                new BigDecimal("10.00"),
                ZonedDateTime.now(),
                null
        );

        assertThatThrownBy(() -> validator.validateTransactionRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Till ID is required");
    }

    @Test
    @DisplayName("Should throw exception when paymentMethod is null")
    void validateTransactionRequest_nullPaymentMethod() {
        TransactionRequest request = buildRequest(
                "STORE-001",
                "TILL-001",
                null,
                new BigDecimal("10.00"),
                ZonedDateTime.now(),
                null
        );

        assertThatThrownBy(() -> validator.validateTransactionRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Payment method is required");
    }

    @Test
    @DisplayName("Should throw exception when paymentMethod is blank")
    void validateTransactionRequest_blankPaymentMethod() {
        TransactionRequest request = buildRequest(
                "STORE-001",
                "TILL-001",
                "  ",
                new BigDecimal("10.00"),
                ZonedDateTime.now(),
                null
        );

        assertThatThrownBy(() -> validator.validateTransactionRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Payment method is required");
    }

    @Test
    @DisplayName("Should throw exception when paymentMethod is invalid")
    void validateTransactionRequest_invalidPaymentMethod() {
        TransactionRequest request = buildRequest(
                "STORE-001",
                "TILL-001",
                "cheque",
                new BigDecimal("10.00"),
                ZonedDateTime.now(),
                null
        );

        assertThatThrownBy(() -> validator.validateTransactionRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Payment method must be 'cash' or 'card'");
    }
    @Test
    @DisplayName("Should throw exception when totalAmount is null")
    void validateTransactionRequest_nullTotalAmount() {
        TransactionRequest request = buildRequest(
                "STORE-001",
                "TILL-001",
                "card",
                null,
                ZonedDateTime.now(),
                null
        );

        assertThatThrownBy(() -> validator.validateTransactionRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Total amount is required");
    }

    @Test
    @DisplayName("Should throw exception when totalAmount is zero")
    void validateTransactionRequest_zeroTotalAmount() {
        TransactionRequest request = buildRequest(
                "STORE-001",
                "TILL-001",
                "card",
                BigDecimal.ZERO,
                ZonedDateTime.now(),
                null
        );

        assertThatThrownBy(() -> validator.validateTransactionRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Total amount must be greater than zero");
    }

    @Test
    @DisplayName("Should throw exception when totalAmount is negative")
    void validateTransactionRequest_negativeTotalAmount() {
        TransactionRequest request = buildRequest(
                "STORE-001",
                "TILL-001",
                "card",
                new BigDecimal("-10.00"),
                ZonedDateTime.now(),
                null
        );

        assertThatThrownBy(() -> validator.validateTransactionRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Total amount must be greater than zero");
    }

    @Test
    @DisplayName("Should throw exception when timestamp is null")
    void validateTransactionRequest_nullTimestamp() {
        TransactionRequest request = buildRequest(
                "STORE-001",
                "TILL-001",
                "card",
                new BigDecimal("10.00"),
                null,
                null
        );

        assertThatThrownBy(() -> validator.validateTransactionRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Transaction creation time is required");
    }

    @Test
    @DisplayName("Should throw ReceiptTotalMismatchException when calculated total doesn't match provided total")
    void validateTransactionRequest_totalMismatch() {
        TransactionItemRequest item = new TransactionItemRequest(
                "Apple",
                null,
                new BigDecimal("5.00"),
                2,
                null
        );
        TransactionRequest request = buildRequest(
                "STORE-001",
                "TILL-001",
                "card",
                new BigDecimal("100.00"),
                ZonedDateTime.now(),
                List.of(item)
        );

        assertThatThrownBy(() -> validator.validateTransactionRequest(request))
                .isInstanceOf(ReceiptTotalMismatchException.class)
                .hasMessageContaining("Receipt total mismatch");
    }

    @Test
    @DisplayName("Should throw exception when item is null")
    void validateTransactionRequest_nullItem() {
        List<TransactionItemRequest> items = new ArrayList<>();
        items.add(null);
        TransactionRequest request = buildRequest(
                "STORE-001",
                "TILL-001",
                "card",
                new BigDecimal("10.00"),
                ZonedDateTime.now(),
                items
        );

        assertThatThrownBy(() -> validator.validateTransactionRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Item at index 0 cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when item unitPrice is null")
    void validateTransactionRequest_nullItemUnitPrice() {
        TransactionItemRequest item = new TransactionItemRequest(
                "Apple",
                null,
                null,
                2,
                null
        );
        TransactionRequest request = buildRequest(
                "STORE-001",
                "TILL-001",
                "card",
                new BigDecimal("10.00"),
                ZonedDateTime.now(),
                List.of(item)
        );

        assertThatThrownBy(() -> validator.validateTransactionRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Item at index 0: Unit price is required");
    }

    @Test
    @DisplayName("Should throw exception when item unitPrice is negative")
    void validateTransactionRequest_negativeItemUnitPrice() {
        TransactionItemRequest item = new TransactionItemRequest(
                "Apple",
                null,
                new BigDecimal("-5.00"),
                2,
                null
        );
        TransactionRequest request = buildRequest(
                "STORE-001",
                "TILL-001",
                "card",
                new BigDecimal("10.00"),
                ZonedDateTime.now(),
                List.of(item)
        );

        assertThatThrownBy(() -> validator.validateTransactionRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Item at index 0: Unit price cannot be negative");
    }

    @Test
    @DisplayName("Should throw exception when item quantity is null")
    void validateTransactionRequest_nullItemQuantity() {
        TransactionItemRequest item = new TransactionItemRequest(
                "Apple",
                null,
                new BigDecimal("5.00"),
                null,
                null
        );
        TransactionRequest request = buildRequest(
                "STORE-001",
                "TILL-001",
                "card",
                new BigDecimal("10.00"),
                ZonedDateTime.now(),
                List.of(item)
        );

        assertThatThrownBy(() -> validator.validateTransactionRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Item at index 0: Quantity is required");
    }

    @Test
    @DisplayName("Should throw exception when item quantity is zero")
    void validateTransactionRequest_zeroItemQuantity() {
        TransactionItemRequest item = new TransactionItemRequest(
                "Apple",
                null,
                new BigDecimal("5.00"),
                0,
                null
        );
        TransactionRequest request = buildRequest(
                "STORE-001",
                "TILL-001",
                "card",
                new BigDecimal("10.00"),
                ZonedDateTime.now(),
                List.of(item)
        );

        assertThatThrownBy(() -> validator.validateTransactionRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Item at index 0: Quantity must be greater than zero");
    }

    @Test
    @DisplayName("Should throw exception when item quantity is negative")
    void validateTransactionRequest_negativeItemQuantity() {
        TransactionItemRequest item = new TransactionItemRequest(
                "Apple",
                null,
                new BigDecimal("5.00"),
                -1,
                null
        );
        TransactionRequest request = buildRequest(
                "STORE-001",
                "TILL-001",
                "card",
                new BigDecimal("10.00"),
                ZonedDateTime.now(),
                List.of(item)
        );

        assertThatThrownBy(() -> validator.validateTransactionRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Item at index 0: Quantity must be greater than zero");
    }

    private TransactionRequest createValidRequest() {
        return buildRequest(
                "STORE-001",
                "TILL-001",
                "card",
                new BigDecimal("10.00"),
                ZonedDateTime.now(),
                null
        );
    }

    private TransactionRequest buildRequest(String storeId, String tillId, String paymentMethod,
                                            BigDecimal totalAmount, ZonedDateTime timestamp,
                                            List<TransactionItemRequest> items) {
        return new TransactionRequest(
                null,
                null,
                storeId,
                tillId,
                paymentMethod,
                totalAmount,
                "GBP",
                timestamp,
                items
        );
    }

    @Nested
    @DisplayName("validateTransactionCommand - Transaction ID Format Tests")
    class TransactionIdFormatTests {

        @Test
        @DisplayName("Should pass validation with valid transaction ID (TXN- prefix with UUID)")
        void validateTransactionCommand_validTransactionId() {
            CreateTransactionCommand command = new CreateTransactionCommand(
                    "TXN-" + UUID.randomUUID().toString().toUpperCase(),
                    "CUST-001",
                    "STORE-001",
                    "TILL-001",
                    "card",
                    new BigDecimal("10.00"),
                    "GBP",
                    Instant.now(),
                    null
            );

            assertDoesNotThrow(() -> validator.validateTransactionCommand(command));
        }

        @Test
        @DisplayName("Should pass validation when transaction ID is null")
        void validateTransactionCommand_nullTransactionId() {
            CreateTransactionCommand command = new CreateTransactionCommand(
                    null,
                    "CUST-001",
                    "STORE-001",
                    "TILL-001",
                    "card",
                    new BigDecimal("10.00"),
                    "GBP",
                    Instant.now(),
                    null
            );

            assertDoesNotThrow(() -> validator.validateTransactionCommand(command));
        }

        @Test
        @DisplayName("Should pass validation when transaction ID is empty string")
        void validateTransactionCommand_emptyTransactionId() {
            CreateTransactionCommand command = new CreateTransactionCommand(
                    "",
                    "CUST-001",
                    "STORE-001",
                    "TILL-001",
                    "card",
                    new BigDecimal("10.00"),
                    "GBP",
                    Instant.now(),
                    null
            );

            assertDoesNotThrow(() -> validator.validateTransactionCommand(command));
        }

        @Test
        @DisplayName("Should throw exception when transaction ID doesn't start with TXN-")
        void validateTransactionCommand_invalidPrefix() {
            CreateTransactionCommand command = new CreateTransactionCommand(
                    "TRANS-" + UUID.randomUUID().toString(),
                    "CUST-001",
                    "STORE-001",
                    "TILL-001",
                    "card",
                    new BigDecimal("10.00"),
                    "GBP",
                    Instant.now(),
                    null
            );

            assertThatThrownBy(() -> validator.validateTransactionCommand(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Transaction ID must start with 'TXN-'");
        }

        @Test
        @DisplayName("Should throw exception when transaction ID has no value after TXN-")
        void validateTransactionCommand_noValueAfterPrefix() {
            CreateTransactionCommand command = new CreateTransactionCommand(
                    "TXN-",
                    "CUST-001",
                    "STORE-001",
                    "TILL-001",
                    "card",
                    new BigDecimal("10.00"),
                    "GBP",
                    Instant.now(),
                    null
            );

            assertThatThrownBy(() -> validator.validateTransactionCommand(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Transaction ID must have a valid UUID after 'TXN-' prefix");
        }

        @Test
        @DisplayName("Should throw exception when transaction ID has invalid UUID format")
        void validateTransactionCommand_invalidUuidFormat() {
            CreateTransactionCommand command = new CreateTransactionCommand(
                    "TXN-NOT-A-UUID",
                    "CUST-001",
                    "STORE-001",
                    "TILL-001",
                    "card",
                    new BigDecimal("10.00"),
                    "GBP",
                    Instant.now(),
                    null
            );

            assertThatThrownBy(() -> validator.validateTransactionCommand(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Transaction ID must have a valid UUID after 'TXN-' prefix");
        }

        @Test
        @DisplayName("Should throw exception when transaction ID has malformed UUID")
        void validateTransactionCommand_malformedUuid() {
            CreateTransactionCommand command = new CreateTransactionCommand(
                    "TXN-12345",
                    "CUST-001",
                    "STORE-001",
                    "TILL-001",
                    "card",
                    new BigDecimal("10.00"),
                    "GBP",
                    Instant.now(),
                    null
            );

            assertThatThrownBy(() -> validator.validateTransactionCommand(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Transaction ID must have a valid UUID after 'TXN-' prefix");
        }

        @Test
        @DisplayName("Should accept transaction ID with lowercase UUID")
        void validateTransactionCommand_lowercaseUuid() {
            CreateTransactionCommand command = new CreateTransactionCommand(
                    "TXN-" + UUID.randomUUID().toString().toLowerCase(),
                    "CUST-001",
                    "STORE-001",
                    "TILL-001",
                    "card",
                    new BigDecimal("10.00"),
                    "GBP",
                    Instant.now(),
                    null
            );

            assertDoesNotThrow(() -> validator.validateTransactionCommand(command));
        }

        @Test
        @DisplayName("Should throw exception when paymentMethod is invalid")
        void validateTransactionCommand_invalidPaymentMethod() {
            CreateTransactionCommand command = new CreateTransactionCommand(
                    "TXN-" + UUID.randomUUID(),
                    "CUST-001",
                    "STORE-001",
                    "TILL-001",
                    "cheque",
                    new BigDecimal("10.00"),
                    "GBP",
                    Instant.now(),
                    null
            );

            assertThatThrownBy(() -> validator.validateTransactionCommand(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Payment method must be 'cash' or 'card'");
        }
    }
}
