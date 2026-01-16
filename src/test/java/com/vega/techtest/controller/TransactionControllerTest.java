package com.vega.techtest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vega.techtest.dto.TransactionItemRequest;
import com.vega.techtest.dto.TransactionItemResponse;
import com.vega.techtest.dto.TransactionRequest;
import com.vega.techtest.dto.TransactionResponse;
import com.vega.techtest.service.TransactionService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@Import(TransactionControllerTest.MeterRegistryTestConfig.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    @Autowired
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        Mockito.reset(transactionService);
    }

    @TestConfiguration
    static class MeterRegistryTestConfig {

        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    @Nested
    @DisplayName("POST /api/transactions/submit")
    class SubmitTransactionTests {

        @Test
        @DisplayName("Should return 200 and increment metrics on successful submission")
        void submitTransaction_success() throws Exception {
            TransactionRequest request = createValidTransactionRequest();
            TransactionResponse response = createTransactionResponse("TXN-001");

            when(transactionService.processTransaction(any(TransactionRequest.class)))
                    .thenReturn(response);

            double counterBefore = getCounterValue("transaction_submissions_total");

            mockMvc.perform(post("/api/transactions/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.message").value("Transaction processed successfully"))
                    .andExpect(jsonPath("$.transactionId").value("TXN-001"))
                    .andExpect(jsonPath("$.timestamp").exists());

            double counterAfter = getCounterValue("transaction_submissions_total");
            assertThat(counterAfter).isEqualTo(counterBefore + 1);

            verify(transactionService).processTransaction(any(TransactionRequest.class));
        }

        @Test
        @DisplayName("Should return 400 and increment error counter on IllegalArgumentException")
        void submitTransaction_invalidRequest() throws Exception {
            TransactionRequest request = createValidTransactionRequest();

            when(transactionService.processTransaction(any(TransactionRequest.class)))
                    .thenThrow(new IllegalArgumentException("Store ID is required"));

            double errorCounterBefore = getCounterValue("transaction_errors_total");

            mockMvc.perform(post("/api/transactions/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Invalid transaction data"))
                    .andExpect(jsonPath("$.error").value("Store ID is required"));

            double errorCounterAfter = getCounterValue("transaction_errors_total");
            assertThat(errorCounterAfter).isEqualTo(errorCounterBefore + 1);
        }

        @Test
        @DisplayName("Should return 500 and increment error counter on other exceptions")
        void submitTransaction_internalError() throws Exception {
            TransactionRequest request = createValidTransactionRequest();

            when(transactionService.processTransaction(any(TransactionRequest.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            double errorCounterBefore = getCounterValue("transaction_errors_total");

            mockMvc.perform(post("/api/transactions/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Failed to process transaction"))
                    .andExpect(jsonPath("$.error").value("Internal server error"));

            double errorCounterAfter = getCounterValue("transaction_errors_total");
            assertThat(errorCounterAfter).isEqualTo(errorCounterBefore + 1);
        }

        @Test
        @DisplayName("Should record metrics with correct tags for store, till, and payment method")
        void submitTransaction_recordsMetricsWithTags() throws Exception {
            TransactionRequest request = createValidTransactionRequest();
            request.setStoreId("STORE-123");
            request.setTillId("TILL-456");
            request.setPaymentMethod("card");

            TransactionResponse response = createTransactionResponse("TXN-001");

            when(transactionService.processTransaction(any(TransactionRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/transactions/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            Counter storeCounter = meterRegistry.find("transaction_submissions_by_store")
                    .tag("store_id", "STORE-123")
                    .counter();
            assertThat(storeCounter).isNotNull();

            Counter tillCounter = meterRegistry.find("transaction_submissions_by_till")
                    .tag("till_id", "TILL-456")
                    .counter();
            assertThat(tillCounter).isNotNull();

            Counter paymentCounter = meterRegistry.find("transaction_submissions_by_payment_method")
                    .tag("payment_method", "card")
                    .counter();
            assertThat(paymentCounter).isNotNull();
        }
    }

    @Nested
    @DisplayName("GET /api/transactions/{transactionId}")
    class GetTransactionByIdTests {

        @Test
        @DisplayName("Should return 200 and transaction when found")
        void getTransaction_found() throws Exception {
            TransactionResponse response = createTransactionResponse("TXN-001");

            when(transactionService.getTransactionById("TXN-001"))
                    .thenReturn(Optional.of(response));

            mockMvc.perform(get("/api/transactions/TXN-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.transactionId").value("TXN-001"))
                    .andExpect(jsonPath("$.storeId").value("STORE-001"))
                    .andExpect(jsonPath("$.customerId").value("CUST-001"));

            verify(transactionService).getTransactionById("TXN-001");
        }

        @Test
        @DisplayName("Should return 404 when transaction not found")
        void getTransaction_notFound() throws Exception {
            when(transactionService.getTransactionById("TXN-999"))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/api/transactions/TXN-999"))
                    .andExpect(status().isNotFound());

            verify(transactionService).getTransactionById("TXN-999");
        }

        @Test
        @DisplayName("Should return 500 on service exception")
        void getTransaction_serviceError() throws Exception {
            when(transactionService.getTransactionById("TXN-001"))
                    .thenThrow(new RuntimeException("Database error"));

            mockMvc.perform(get("/api/transactions/TXN-001"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Failed to retrieve transaction"))
                    .andExpect(jsonPath("$.error").value("Internal server error"));
        }
    }

    @Nested
    @DisplayName("GET /api/transactions/store/{storeId}")
    class GetTransactionsByStoreTests {

        @Test
        @DisplayName("Should return 200 with transactions list")
        void getTransactionsByStore_success() throws Exception {
            List<TransactionResponse> transactions = Arrays.asList(
                    createTransactionResponse("TXN-001"),
                    createTransactionResponse("TXN-002")
            );

            when(transactionService.getTransactionsByStore("STORE-001"))
                    .thenReturn(transactions);

            mockMvc.perform(get("/api/transactions/store/STORE-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.storeId").value("STORE-001"))
                    .andExpect(jsonPath("$.count").value(2))
                    .andExpect(jsonPath("$.transactions", hasSize(2)))
                    .andExpect(jsonPath("$.transactions[0].transactionId").value("TXN-001"))
                    .andExpect(jsonPath("$.transactions[1].transactionId").value("TXN-002"));

            verify(transactionService).getTransactionsByStore("STORE-001");
        }

        @Test
        @DisplayName("Should return 500 on service exception")
        void getTransactionsByStore_serviceError() throws Exception {
            when(transactionService.getTransactionsByStore("STORE-001"))
                    .thenThrow(new RuntimeException("Database error"));

            mockMvc.perform(get("/api/transactions/store/STORE-001"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Failed to retrieve transactions"))
                    .andExpect(jsonPath("$.error").value("Internal server error"));
        }
    }

    @Nested
    @DisplayName("GET /api/transactions/customer/{customerId}")
    class GetTransactionsByCustomerTests {

        @Test
        @DisplayName("Should return 200 with transactions list")
        void getTransactionsByCustomer_success() throws Exception {
            List<TransactionResponse> transactions = Arrays.asList(
                    createTransactionResponse("TXN-001"),
                    createTransactionResponse("TXN-002")
            );

            when(transactionService.getTransactionsByCustomer("CUST-001"))
                    .thenReturn(transactions);

            mockMvc.perform(get("/api/transactions/customer/CUST-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.customerId").value("CUST-001"))
                    .andExpect(jsonPath("$.count").value(2))
                    .andExpect(jsonPath("$.transactions", hasSize(2)));

            verify(transactionService).getTransactionsByCustomer("CUST-001");
        }

        @Test
        @DisplayName("Should return 500 on service exception")
        void getTransactionsByCustomer_serviceError() throws Exception {
            when(transactionService.getTransactionsByCustomer("CUST-001"))
                    .thenThrow(new RuntimeException("Database error"));

            mockMvc.perform(get("/api/transactions/customer/CUST-001"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Failed to retrieve transactions"))
                    .andExpect(jsonPath("$.error").value("Internal server error"));
        }
    }

    @Nested
    @DisplayName("GET /api/transactions/till/{tillId}")
    class GetTransactionsByTillTests {

        @Test
        @DisplayName("Should return 200 with transactions list")
        void getTransactionsByTill_success() throws Exception {
            List<TransactionResponse> transactions = Arrays.asList(
                    createTransactionResponse("TXN-001"),
                    createTransactionResponse("TXN-002")
            );

            when(transactionService.getTransactionsByTill("TILL-001"))
                    .thenReturn(transactions);

            mockMvc.perform(get("/api/transactions/till/TILL-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tillId").value("TILL-001"))
                    .andExpect(jsonPath("$.count").value(2))
                    .andExpect(jsonPath("$.transactions", hasSize(2)));

            verify(transactionService).getTransactionsByTill("TILL-001");
        }

        @Test
        @DisplayName("Should return 500 on service exception")
        void getTransactionsByTill_serviceError() throws Exception {
            when(transactionService.getTransactionsByTill("TILL-001"))
                    .thenThrow(new RuntimeException("Database error"));

            mockMvc.perform(get("/api/transactions/till/TILL-001"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Failed to retrieve transactions"))
                    .andExpect(jsonPath("$.error").value("Internal server error"));
        }
    }

    @Nested
    @DisplayName("GET /api/transactions/date-range")
    class GetTransactionsByDateRangeTests {

        @Test
        @DisplayName("Should return 200 with transactions in date range")
        void getTransactionsByDateRange_success() throws Exception {
            List<TransactionResponse> transactions = Arrays.asList(
                    createTransactionResponse("TXN-001"),
                    createTransactionResponse("TXN-002")
            );

            when(transactionService.getTransactionsByDateRange(any(ZonedDateTime.class), any(ZonedDateTime.class)))
                    .thenReturn(transactions);

            mockMvc.perform(get("/api/transactions/date-range")
                            .param("startDate", "2024-01-01T00:00:00Z")
                            .param("endDate", "2024-01-31T23:59:59Z"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.startDate").exists())
                    .andExpect(jsonPath("$.endDate").exists())
                    .andExpect(jsonPath("$.count").value(2))
                    .andExpect(jsonPath("$.transactions", hasSize(2)));

            verify(transactionService).getTransactionsByDateRange(any(ZonedDateTime.class), any(ZonedDateTime.class));
        }

        @Test
        @DisplayName("Should return 400 on invalid date format")
        void getTransactionsByDateRange_invalidDateFormat() throws Exception {
            mockMvc.perform(get("/api/transactions/date-range")
                            .param("startDate", "invalid-date")
                            .param("endDate", "2024-01-31T23:59:59Z"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 500 on service exception")
        void getTransactionsByDateRange_serviceError() throws Exception {
            when(transactionService.getTransactionsByDateRange(any(ZonedDateTime.class), any(ZonedDateTime.class)))
                    .thenThrow(new RuntimeException("Database error"));

            mockMvc.perform(get("/api/transactions/date-range")
                            .param("startDate", "2024-01-01T00:00:00Z")
                            .param("endDate", "2024-01-31T23:59:59Z"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Failed to retrieve transactions"))
                    .andExpect(jsonPath("$.error").value("Internal server error"));
        }
    }

    @Nested
    @DisplayName("POST /api/transactions/sample")
    class CreateSampleTransactionTests {

        @Test
        @DisplayName("Should return 200 and create sample transaction")
        void createSampleTransaction_success() throws Exception {
            TransactionResponse response = createTransactionResponse("TXN-SAMPLE-001");

            when(transactionService.processTransaction(any(TransactionRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/transactions/sample"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.message").value("Sample transaction created"))
                    .andExpect(jsonPath("$.transaction").exists())
                    .andExpect(jsonPath("$.transaction.transactionId").value("TXN-SAMPLE-001"));

            verify(transactionService).processTransaction(any(TransactionRequest.class));
        }

        @Test
        @DisplayName("Should return 500 on service exception")
        void createSampleTransaction_serviceError() throws Exception {
            when(transactionService.processTransaction(any(TransactionRequest.class)))
                    .thenThrow(new RuntimeException("Database error"));

            mockMvc.perform(post("/api/transactions/sample"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Failed to create sample transaction"))
                    .andExpect(jsonPath("$.error").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/transactions/health")
    class HealthCheckTests {

        @Test
        @DisplayName("Should always return 200 with UP status")
        void health_alwaysReturns200() throws Exception {
            mockMvc.perform(get("/api/transactions/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.service").value("transaction-service"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/transactions/stats/{storeId}")
    class GetTransactionStatsTests {

        @Test
        @DisplayName("Should return 200 with calculated statistics")
        void getTransactionStats_success() throws Exception {
            List<TransactionResponse> transactions = Arrays.asList(
                    createTransactionResponseWithAmount("TXN-001", new BigDecimal("100.00")),
                    createTransactionResponseWithAmount("TXN-002", new BigDecimal("200.00"))
            );

            when(transactionService.getTransactionsByStore("STORE-001"))
                    .thenReturn(transactions);

            mockMvc.perform(get("/api/transactions/stats/STORE-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.storeId").value("STORE-001"))
                    .andExpect(jsonPath("$.totalTransactions").value(2))
                    .andExpect(jsonPath("$.totalAmount").value(300.0))
                    .andExpect(jsonPath("$.averageAmount").value(150.0))
                    .andExpect(jsonPath("$.calculationNote").exists());

            verify(transactionService).getTransactionsByStore("STORE-001");
        }

        @Test
        @DisplayName("Should return 200 with zeroed totals when no transactions found")
        void getTransactionStats_emptyList() throws Exception {
            when(transactionService.getTransactionsByStore("STORE-999"))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/transactions/stats/STORE-999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.storeId").value("STORE-999"))
                    .andExpect(jsonPath("$.message").value("No transactions found for this store"))
                    .andExpect(jsonPath("$.totalTransactions").value(0))
                    .andExpect(jsonPath("$.totalAmount").value(0.0))
                    .andExpect(jsonPath("$.averageAmount").value(0.0));

            verify(transactionService).getTransactionsByStore("STORE-999");
        }

        @Test
        @DisplayName("Should return 500 on service exception")
        void getTransactionStats_serviceError() throws Exception {
            when(transactionService.getTransactionsByStore("STORE-001"))
                    .thenThrow(new RuntimeException("Database error"));

            mockMvc.perform(get("/api/transactions/stats/STORE-001"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Failed to calculate transaction statistics"))
                    .andExpect(jsonPath("$.error").value("Internal server error"));
        }
    }

    private TransactionRequest createValidTransactionRequest() {
        TransactionRequest request = new TransactionRequest();
        request.setCustomerId("CUST-001");
        request.setStoreId("STORE-001");
        request.setTillId("TILL-001");
        request.setPaymentMethod("card");
        request.setTotalAmount(new BigDecimal("25.50"));
        request.setCurrency("GBP");
        request.setTimestamp(ZonedDateTime.now());

        TransactionItemRequest item1 = new TransactionItemRequest(
                "Apple", "APPLE-001", new BigDecimal("1.50"), 2, "Fruit"
        );
        TransactionItemRequest item2 = new TransactionItemRequest(
                "Milk", "MILK-001", new BigDecimal("2.50"), 1, "Dairy"
        );
        request.setItems(Arrays.asList(item1, item2));

        return request;
    }

    private TransactionResponse createTransactionResponse(String transactionId) {
        return createTransactionResponseWithAmount(transactionId, new BigDecimal("25.50"));
    }

    private TransactionResponse createTransactionResponseWithAmount(String transactionId, BigDecimal amount) {
        TransactionResponse response = new TransactionResponse(
                transactionId,
                "CUST-001",
                "STORE-001",
                "TILL-001",
                "card",
                amount,
                "GBP",
                ZonedDateTime.now(),
                "COMPLETED"
        );

        TransactionItemResponse item1 = new TransactionItemResponse(
                "Apple", "APPLE-001", new BigDecimal("1.50"), 2, new BigDecimal("3.00"), "Fruit"
        );
        TransactionItemResponse item2 = new TransactionItemResponse(
                "Milk", "MILK-001", new BigDecimal("2.50"), 1, new BigDecimal("2.50"), "Dairy"
        );
        response.setItems(Arrays.asList(item1, item2));

        return response;
    }

    private double getCounterValue(String counterName) {
        Counter counter = meterRegistry.find(counterName).counter();
        return counter != null ? counter.count() : 0.0;
    }
}
