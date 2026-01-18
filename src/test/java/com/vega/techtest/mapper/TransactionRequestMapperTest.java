package com.vega.techtest.mapper;

import com.vega.techtest.dto.TransactionItemRequest;
import com.vega.techtest.dto.TransactionRequest;
import com.vega.techtest.dto.TransactionResponse;
import com.vega.techtest.service.command.CreateTransactionCommand;
import com.vega.techtest.service.command.TransactionItem;
import com.vega.techtest.service.command.TransactionResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TransactionRequestMapperTest {

    @Autowired
    private TransactionRequestMapper mapper;

    @Test
    @DisplayName("Should map TransactionRequest to CreateTransactionCommand")
    void shouldMapTransactionRequestToCommand() {
        ZonedDateTime timestamp = ZonedDateTime.of(2024, 1, 1, 10, 15, 30, 0, ZoneId.of("America/New_York"));
        TransactionRequest request = new TransactionRequest(
                "TXN-123",
                "CUST-1",
                "STORE-001",
                "TILL-1",
                "card",
                new BigDecimal("100.00"),
                "GBP",
                timestamp,
                null
        );

        CreateTransactionCommand command = mapper.toCommand(request);

        assertThat(command.transactionId()).isEqualTo("TXN-123");
        assertThat(command.customerId()).isEqualTo("CUST-1");
        assertThat(command.storeId()).isEqualTo("STORE-001");
        assertThat(command.tillId()).isEqualTo("TILL-1");
        assertThat(command.paymentMethod()).isEqualTo("card");
        assertThat(command.totalAmount()).isEqualByComparingTo("100.00");
        assertThat(command.currency()).isEqualTo("GBP");
        assertThat(command.timestamp()).isEqualTo(timestamp.toInstant());
    }

    @Test
    @DisplayName("Should map null transactionId")
    void shouldMapNullTransactionId() {
        TransactionRequest request = new TransactionRequest(
                null,
                null,
                "STORE-001",
                "TILL-1",
                "card",
                new BigDecimal("100.00"),
                "GBP",
                ZonedDateTime.now(),
                null
        );

        CreateTransactionCommand command = mapper.toCommand(request);

        assertThat(command.transactionId()).isNull();
    }

    @Test
    @DisplayName("Should handle null request")
    void shouldHandleNullRequest() {
        CreateTransactionCommand command = mapper.toCommand(null);

        assertThat(command).isNull();
    }

    @Test
    @DisplayName("Should map TransactionItemRequest to TransactionItem")
    void shouldMapTransactionItemRequestToTransactionItem() {
        TransactionItemRequest itemRequest = new TransactionItemRequest(
            "Product A",
            "PROD-001",
            new BigDecimal("25.50"),
            2,
            "Electronics"
        );

        TransactionItem item = mapper.toCommandItem(itemRequest);

        assertThat(item.productName()).isEqualTo("Product A");
        assertThat(item.productCode()).isEqualTo("PROD-001");
        assertThat(item.unitPrice()).isEqualByComparingTo("25.50");
        assertThat(item.quantity()).isEqualTo(2);
        assertThat(item.category()).isEqualTo("Electronics");
    }

    @Test
    @DisplayName("Should map list of items")
    void shouldMapListOfItems() {
        List<TransactionItemRequest> itemRequests = new ArrayList<>();
        itemRequests.add(new TransactionItemRequest("Product A", "PROD-001", new BigDecimal("25.50"), 2, "Electronics"));
        itemRequests.add(new TransactionItemRequest("Product B", "PROD-002", new BigDecimal("10.00"), 1, "Books"));

        List<TransactionItem> items = mapper.toCommandItems(itemRequests);

        assertThat(items).hasSize(2);
        assertThat(items.get(0).productName()).isEqualTo("Product A");
        assertThat(items.get(0).productCode()).isEqualTo("PROD-001");
        assertThat(items.get(1).productName()).isEqualTo("Product B");
        assertThat(items.get(1).productCode()).isEqualTo("PROD-002");
    }

    @Test
    @DisplayName("Should handle null items list")
    void shouldHandleNullItemsList() {
        List<TransactionItem> items = mapper.toCommandItems(null);

        assertThat(items).isNull();
    }

    @Test
    @DisplayName("Should handle empty items list")
    void shouldHandleEmptyItemsList() {
        List<TransactionItem> items = mapper.toCommandItems(new ArrayList<>());

        assertThat(items).isEmpty();
    }

    @Test
    @DisplayName("Should map request with items to command with items")
    void shouldMapRequestWithItemsToCommandWithItems() {
        ZonedDateTime timestamp = ZonedDateTime.now();
        TransactionRequest request = new TransactionRequest(
                null,
                null,
                "STORE-001",
                "TILL-1",
                "card",
                new BigDecimal("100.00"),
                "GBP",
                timestamp,
                null
        );

        List<TransactionItemRequest> itemRequests = new ArrayList<>();
        itemRequests.add(new TransactionItemRequest("Product A", "PROD-001", new BigDecimal("50.00"), 2));
        request = new TransactionRequest(
                request.transactionId(),
                request.customerId(),
                request.storeId(),
                request.tillId(),
                request.paymentMethod(),
                request.totalAmount(),
                request.currency(),
                request.timestamp(),
                itemRequests
        );

        CreateTransactionCommand command = mapper.toCommand(request);

        assertThat(command.items()).hasSize(1);
        assertThat(command.items().get(0).productName()).isEqualTo("Product A");
        assertThat(command.items().get(0).productCode()).isEqualTo("PROD-001");
    }

    @Test
    @DisplayName("Should preserve currency default from request")
    void shouldPreserveCurrencyDefault() {
        TransactionRequest request = new TransactionRequest(
                null,
                null,
                "STORE-001",
                "TILL-1",
                "card",
                new BigDecimal("100.00"),
                null,
                ZonedDateTime.now(),
                null
        );

        CreateTransactionCommand command = mapper.toCommand(request);

        assertThat(command.currency()).isEqualTo("GBP");
    }

    @Test
    @DisplayName("Should normalize response timestamps to UTC")
    void shouldNormalizeResponseTimestampsToUtc() {
        ZonedDateTime transactionTimestamp = ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneId.of("Asia/Kolkata"));
        ZonedDateTime createdAt = ZonedDateTime.of(2024, 1, 1, 12, 5, 0, 0, ZoneId.of("Asia/Kolkata"));

        TransactionResult result = new TransactionResult(
                "TXN-123",
                "CUST-1",
                "STORE-001",
                "TILL-1",
                "card",
                new BigDecimal("100.00"),
                "GBP",
                transactionTimestamp.toInstant(),
                createdAt.toInstant(),
                "COMPLETED",
                new ArrayList<>()
        );

        TransactionResponse response = mapper.toResponse(result);

        assertThat(response.transactionTimestamp().toInstant()).isEqualTo(transactionTimestamp.toInstant());
        assertThat(response.transactionTimestamp().getZone()).isEqualTo(ZoneOffset.UTC);
        assertThat(response.createdAt().toInstant()).isEqualTo(createdAt.toInstant());
        assertThat(response.createdAt().getZone()).isEqualTo(ZoneOffset.UTC);
    }
}
