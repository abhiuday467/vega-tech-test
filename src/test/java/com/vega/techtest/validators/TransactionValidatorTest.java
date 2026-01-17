package com.vega.techtest.validators;

import com.vega.techtest.dto.TransactionItemRequest;
import com.vega.techtest.dto.TransactionRequest;
import com.vega.techtest.exception.ReceiptTotalMismatchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

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
        TransactionRequest request = createValidRequest();
        request.setStoreId(null);

        assertThatThrownBy(() -> validator.validateTransactionRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Store ID is required");
    }

    @Test
    @DisplayName("Should throw exception when storeId is blank")
    void validateTransactionRequest_blankStoreId() {
        TransactionRequest request = createValidRequest();
        request.setStoreId("   ");

        assertThatThrownBy(() -> validator.validateTransactionRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Store ID is required");
    }

    @Test
    @DisplayName("Should throw exception when tillId is null")
    void validateTransactionRequest_nullTillId() {
        TransactionRequest request = createValidRequest();
        request.setTillId(null);

        assertThatThrownBy(() -> validator.validateTransactionRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Till ID is required");
    }

    @Test
    @DisplayName("Should throw exception when tillId is blank")
    void validateTransactionRequest_blankTillId() {
        TransactionRequest request = createValidRequest();
        request.setTillId("");

        assertThatThrownBy(() -> validator.validateTransactionRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Till ID is required");
    }

    @Test
    @DisplayName("Should throw exception when paymentMethod is null")
    void validateTransactionRequest_nullPaymentMethod() {
        TransactionRequest request = createValidRequest();
        request.setPaymentMethod(null);

        assertThatThrownBy(() -> validator.validateTransactionRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Payment method is required");
    }

    @Test
    @DisplayName("Should throw exception when paymentMethod is blank")
    void validateTransactionRequest_blankPaymentMethod() {
        TransactionRequest request = createValidRequest();
        request.setPaymentMethod("  ");

        assertThatThrownBy(() -> validator.validateTransactionRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Payment method is required");
    }

    @Test
    @DisplayName("Should throw exception when totalAmount is null")
    void validateTransactionRequest_nullTotalAmount() {
        TransactionRequest request = createValidRequest();
        request.setTotalAmount(null);

        assertThatThrownBy(() -> validator.validateTransactionRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Total amount is required");
    }

    @Test
    @DisplayName("Should throw exception when totalAmount is zero")
    void validateTransactionRequest_zeroTotalAmount() {
        TransactionRequest request = createValidRequest();
        request.setTotalAmount(BigDecimal.ZERO);

        assertThatThrownBy(() -> validator.validateTransactionRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Total amount must be greater than zero");
    }

    @Test
    @DisplayName("Should throw exception when totalAmount is negative")
    void validateTransactionRequest_negativeTotalAmount() {
        TransactionRequest request = createValidRequest();
        request.setTotalAmount(new BigDecimal("-10.00"));

        assertThatThrownBy(() -> validator.validateTransactionRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Total amount must be greater than zero");
    }

    @Test
    @DisplayName("Should throw exception when timestamp is null")
    void validateTransactionRequest_nullTimestamp() {
        TransactionRequest request = createValidRequest();
        request.setTimestamp(null);

        assertThatThrownBy(() -> validator.validateTransactionRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Transaction creation time is required");
    }

    @Test
    @DisplayName("Should throw ReceiptTotalMismatchException when calculated total doesn't match provided total")
    void validateTransactionRequest_totalMismatch() {
        TransactionRequest request = createValidRequest();
        request.setTotalAmount(new BigDecimal("100.00")); // Doesn't match items total
        TransactionItemRequest item = new TransactionItemRequest();
        item.setProductName("Apple");
        item.setUnitPrice(new BigDecimal("5.00"));
        item.setQuantity(2);
        request.setItems(List.of(item));

        assertThatThrownBy(() -> validator.validateTransactionRequest(request))
                .isInstanceOf(ReceiptTotalMismatchException.class)
                .hasMessageContaining("Receipt total mismatch");
    }

    @Test
    @DisplayName("Should throw exception when item is null")
    void validateTransactionRequest_nullItem() {
        TransactionRequest request = createValidRequest();
        List<TransactionItemRequest> items = new ArrayList<>();
        items.add(null);
        request.setItems(items);

        assertThatThrownBy(() -> validator.validateTransactionRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Item at index 0 cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when item unitPrice is null")
    void validateTransactionRequest_nullItemUnitPrice() {
        TransactionRequest request = createValidRequest();
        TransactionItemRequest item = new TransactionItemRequest();
        item.setProductName("Apple");
        item.setUnitPrice(null);
        item.setQuantity(2);
        request.setItems(List.of(item));

        assertThatThrownBy(() -> validator.validateTransactionRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Item at index 0: Unit price is required");
    }

    @Test
    @DisplayName("Should throw exception when item unitPrice is negative")
    void validateTransactionRequest_negativeItemUnitPrice() {
        TransactionRequest request = createValidRequest();
        TransactionItemRequest item = new TransactionItemRequest();
        item.setProductName("Apple");
        item.setUnitPrice(new BigDecimal("-5.00"));
        item.setQuantity(2);
        request.setItems(List.of(item));

        assertThatThrownBy(() -> validator.validateTransactionRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Item at index 0: Unit price cannot be negative");
    }

    @Test
    @DisplayName("Should throw exception when item quantity is null")
    void validateTransactionRequest_nullItemQuantity() {
        TransactionRequest request = createValidRequest();
        TransactionItemRequest item = new TransactionItemRequest();
        item.setProductName("Apple");
        item.setUnitPrice(new BigDecimal("5.00"));
        item.setQuantity(null);
        request.setItems(List.of(item));

        assertThatThrownBy(() -> validator.validateTransactionRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Item at index 0: Quantity is required");
    }

    @Test
    @DisplayName("Should throw exception when item quantity is zero")
    void validateTransactionRequest_zeroItemQuantity() {
        TransactionRequest request = createValidRequest();
        TransactionItemRequest item = new TransactionItemRequest();
        item.setProductName("Apple");
        item.setUnitPrice(new BigDecimal("5.00"));
        item.setQuantity(0);
        request.setItems(List.of(item));

        assertThatThrownBy(() -> validator.validateTransactionRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Item at index 0: Quantity must be greater than zero");
    }

    @Test
    @DisplayName("Should throw exception when item quantity is negative")
    void validateTransactionRequest_negativeItemQuantity() {
        TransactionRequest request = createValidRequest();
        TransactionItemRequest item = new TransactionItemRequest();
        item.setProductName("Apple");
        item.setUnitPrice(new BigDecimal("5.00"));
        item.setQuantity(-1);
        request.setItems(List.of(item));

        assertThatThrownBy(() -> validator.validateTransactionRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Item at index 0: Quantity must be greater than zero");
    }

    private TransactionRequest createValidRequest() {
        TransactionRequest request = new TransactionRequest();
        request.setStoreId("STORE-001");
        request.setTillId("TILL-001");
        request.setPaymentMethod("card");
        request.setTotalAmount(new BigDecimal("10.00"));
        request.setTimestamp(ZonedDateTime.now());
        return request;
    }
}
