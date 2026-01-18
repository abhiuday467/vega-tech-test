package com.vega.techtest.service;

import com.vega.techtest.entity.TransactionEntity;
import com.vega.techtest.entity.TransactionItemEntity;
import com.vega.techtest.mapper.TransactionEntityMapper;
import com.vega.techtest.repository.TransactionRepository;
import com.vega.techtest.service.command.CreateTransactionCommand;
import com.vega.techtest.service.command.TransactionItem;
import com.vega.techtest.service.command.TransactionResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DuplicateTransactionHandlerTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionEntityMapper mapper;

    @InjectMocks
    private DuplicateTransactionHandler handler;

    @Test
    void returnsExistingTransactionWhenPayloadMatches() {
        Instant timestamp = Instant.parse("2024-01-01T10:15:30Z");
        CreateTransactionCommand command = new CreateTransactionCommand(
                null,
                "CUST-1",
                "STORE-001",
                "TILL-001",
                "card",
                new BigDecimal("10.00"),
                "GBP",
                timestamp,
                List.of(new TransactionItem("Apple", "APPLE-001", new BigDecimal("5.00"), 2, "Fruit"))
        );

        TransactionEntity existing = buildMatchingEntity(command);
        TransactionResult expected = new TransactionResult(
                "TXN-123",
                "CUST-1",
                "STORE-001",
                "TILL-001",
                "card",
                new BigDecimal("10.00"),
                "GBP",
                timestamp,
                timestamp,
                "COMPLETED",
                List.of()
        );

        when(transactionRepository.findByStoreIdAndTillIdAndTransactionTimestamp(
                command.storeId(), command.tillId(), command.timestamp()))
                .thenReturn(existing);
        when(mapper.toResult(existing)).thenReturn(expected);

        TransactionResult result = handler.findExistingTransaction(command);

        assertThat(result).isEqualTo(expected);
        verify(mapper).toResult(existing);
    }

    @Test
    void throwsWhenPayloadDoesNotMatch() {
        Instant timestamp = Instant.parse("2024-01-01T10:15:30Z");
        CreateTransactionCommand command = new CreateTransactionCommand(
                null,
                "CUST-1",
                "STORE-001",
                "TILL-001",
                "card",
                new BigDecimal("10.00"),
                "GBP",
                timestamp,
                List.of(new TransactionItem("Apple", "APPLE-001", new BigDecimal("5.00"), 2, "Fruit"))
        );

        TransactionEntity existing = buildMatchingEntity(command);
        existing.setPaymentMethod("cash");

        when(transactionRepository.findByStoreIdAndTillIdAndTransactionTimestamp(
                command.storeId(), command.tillId(), command.timestamp()))
                .thenReturn(existing);

        assertThatThrownBy(() -> handler.findExistingTransaction(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("URGENT- Bad Transactions")
                .hasMessageContaining("paymentMethod");

        verify(mapper, never()).toResult(any());
    }

    @Test
    void throwsWhenCustomerIdDiffers() {
        assertMismatch("customerId", entity -> entity.setCustomerId("CUST-2"));
    }

    @Test
    void throwsWhenStoreIdDiffers() {
        assertMismatch("storeId", entity -> entity.setStoreId("STORE-002"));
    }

    @Test
    void throwsWhenTillIdDiffers() {
        assertMismatch("tillId", entity -> entity.setTillId("TILL-002"));
    }

    @Test
    void throwsWhenPaymentMethodDiffers() {
        assertMismatch("paymentMethod", entity -> entity.setPaymentMethod("cash"));
    }

    @Test
    void throwsWhenCurrencyDiffers() {
        assertMismatch("currency", entity -> entity.setCurrency("USD"));
    }

    @Test
    void throwsWhenTotalAmountDiffers() {
        assertMismatch("totalAmount", entity -> entity.setTotalAmount(new BigDecimal("11.00")));
    }

    @Test
    void throwsWhenTimestampDiffers() {
        assertMismatch("timestamp", entity -> entity.setTransactionTimestamp(
                Instant.parse("2024-01-01T10:15:31Z")));
    }

    @Test
    void throwsWhenItemProductNameDiffers() {
        assertItemMismatch("items[0].productName", item -> item.setProductName("Orange"));
    }

    @Test
    void throwsWhenItemProductCodeDiffers() {
        assertItemMismatch("items[0].productCode", item -> item.setProductCode("ORANGE-001"));
    }

    @Test
    void throwsWhenItemUnitPriceDiffers() {
        assertItemMismatch("items[0].unitPrice", item -> item.setUnitPrice(new BigDecimal("6.00")));
    }

    @Test
    void throwsWhenItemQuantityDiffers() {
        assertItemMismatch("items[0].quantity", item -> item.setQuantity(3));
    }

    @Test
    void throwsWhenItemCategoryDiffers() {
        assertItemMismatch("items[0].category", item -> item.setCategory("Fresh"));
    }

    private void assertMismatch(String fieldName, java.util.function.Consumer<TransactionEntity> mutator) {
        Instant timestamp = Instant.parse("2024-01-01T10:15:30Z");
        CreateTransactionCommand command = new CreateTransactionCommand(
                null,
                "CUST-1",
                "STORE-001",
                "TILL-001",
                "card",
                new BigDecimal("10.00"),
                "GBP",
                timestamp,
                List.of(new TransactionItem("Apple", "APPLE-001", new BigDecimal("5.00"), 2, "Fruit"))
        );

        TransactionEntity existing = buildMatchingEntity(command);
        mutator.accept(existing);

        when(transactionRepository.findByStoreIdAndTillIdAndTransactionTimestamp(
                command.storeId(), command.tillId(), command.timestamp()))
                .thenReturn(existing);

        assertThatThrownBy(() -> handler.findExistingTransaction(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("URGENT- Bad Transactions")
                .hasMessageContaining(fieldName);

        verify(mapper, never()).toResult(any());
        reset(transactionRepository, mapper);
    }

    private void assertItemMismatch(String fieldName, java.util.function.Consumer<TransactionItemEntity> mutator) {
        Instant timestamp = Instant.parse("2024-01-01T10:15:30Z");
        CreateTransactionCommand command = new CreateTransactionCommand(
                null,
                "CUST-1",
                "STORE-001",
                "TILL-001",
                "card",
                new BigDecimal("10.00"),
                "GBP",
                timestamp,
                List.of(new TransactionItem("Apple", "APPLE-001", new BigDecimal("5.00"), 2, "Fruit"))
        );

        TransactionEntity existing = buildMatchingEntity(command);
        TransactionItemEntity existingItem = existing.getItems().get(0);
        mutator.accept(existingItem);

        when(transactionRepository.findByStoreIdAndTillIdAndTransactionTimestamp(
                command.storeId(), command.tillId(), command.timestamp()))
                .thenReturn(existing);

        assertThatThrownBy(() -> handler.findExistingTransaction(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("URGENT- Bad Transactions")
                .hasMessageContaining(fieldName);

        verify(mapper, never()).toResult(any());
        reset(transactionRepository, mapper);
    }

    private TransactionEntity buildMatchingEntity(CreateTransactionCommand command) {
        TransactionEntity entity = new TransactionEntity();
        entity.setTransactionId("TXN-123");
        entity.setCustomerId(command.customerId());
        entity.setStoreId(command.storeId());
        entity.setTillId(command.tillId());
        entity.setPaymentMethod(command.paymentMethod());
        entity.setTotalAmount(command.totalAmount());
        entity.setCurrency(command.currency());
        entity.setTransactionTimestamp(command.timestamp());
        entity.setCreatedAt(command.timestamp());
        entity.setStatus("COMPLETED");

        TransactionItemEntity item = new TransactionItemEntity();
        item.setTransaction(entity);
        TransactionItem sent = command.items().get(0);
        item.setProductName(sent.productName());
        item.setProductCode(sent.productCode());
        item.setUnitPrice(sent.unitPrice());
        item.setQuantity(sent.quantity());
        item.setCategory(sent.category());
        item.setTotalPrice(sent.unitPrice().multiply(BigDecimal.valueOf(sent.quantity())));
        entity.setItems(List.of(item));

        return entity;
    }
}
