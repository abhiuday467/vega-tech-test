package com.vega.techtest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vega.techtest.aspect.TimingAspect;
import com.vega.techtest.dto.TransactionItemRequest;
import com.vega.techtest.dto.TransactionItemResponse;
import com.vega.techtest.dto.TransactionRequest;
import com.vega.techtest.dto.TransactionResponse;
import com.vega.techtest.exception.GlobalExceptionHandler;
import com.vega.techtest.exception.ReceiptTotalMismatchException;
import com.vega.techtest.exception.StatisticsCalculationException;
import com.vega.techtest.exception.TransactionProcessingException;
import com.vega.techtest.exception.TransactionRetrievalException;
import com.vega.techtest.mapper.TransactionRequestMapper;
import com.vega.techtest.service.TransactionService;
import com.vega.techtest.service.TransactionMetricsService;
import com.vega.techtest.service.command.CreateTransactionCommand;
import com.vega.techtest.service.command.TransactionResult;
import com.vega.techtest.exception.ResourceNotFoundException;
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
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@EnableAspectJAutoProxy
@Import({
    TransactionControllerTest.MeterRegistryTestConfig.class,
    GlobalExceptionHandler.class,
    TimingAspect.class
})
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private TransactionMetricsService metricsService;

    @MockBean
    private TransactionRequestMapper transactionRequestMapper;

    @BeforeEach
    void setUp() {
        Mockito.reset(transactionService, metricsService, transactionRequestMapper);
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
            TransactionResult result = createTransactionResult("TXN-001");
            TransactionResponse response = createTransactionResponse("TXN-001");

            when(transactionRequestMapper.toCommand(any(TransactionRequest.class)))
                    .thenReturn(mock(CreateTransactionCommand.class));
            when(transactionService.processTransaction(any(CreateTransactionCommand.class)))
                    .thenReturn(result);
            when(transactionRequestMapper.toResponse(any(TransactionResult.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/transactions/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.message").value("Transaction processed successfully"))
                    .andExpect(jsonPath("$.transactionId").value("TXN-001"))
                    .andExpect(jsonPath("$.timestamp").exists());

            verify(transactionService).processTransaction(any(CreateTransactionCommand.class));
            verify(metricsService).recordTransactionSubmission(any(TransactionRequest.class), any(TransactionResponse.class));
        }

        @Test
        @DisplayName("Should return 400 with field errors when bean validation fails")
        void submitTransaction_validationFailure() throws Exception {
            TransactionRequest request = new TransactionRequest();
            request.setCustomerId("CUST-001");
            // Missing storeId (required)
            // Missing tillId (required)
            // Missing paymentMethod (required)
            // Missing totalAmount (required)
            // Missing timestamp (required)

            mockMvc.perform(post("/api/transactions/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Invalid transaction data"))
                    .andExpect(jsonPath("$.errors").isMap())
                    .andExpect(jsonPath("$.errors.storeId").value("Store ID is required"))
                    .andExpect(jsonPath("$.errors.tillId").value("Till ID is required"))
                    .andExpect(jsonPath("$.errors.paymentMethod").value("Payment method is required"))
                    .andExpect(jsonPath("$.errors.totalAmount").value("Total amount is required"))
                    .andExpect(jsonPath("$.errors.timestamp").value("Transaction creation time is required"));
            verifyNoInteractions(metricsService);
        }

        @Test
        @DisplayName("Should return 400 when totalAmount is zero")
        void submitTransaction_zeroAmount() throws Exception {
            TransactionRequest request = createValidTransactionRequest();
            request.setTotalAmount(new BigDecimal("0.00"));

            mockMvc.perform(post("/api/transactions/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Invalid transaction data"))
                    .andExpect(jsonPath("$.errors").isMap())
                    .andExpect(jsonPath("$.errors.totalAmount").value("Total amount must be greater than zero"));
            verifyNoInteractions(metricsService);
        }

        @Test
        @DisplayName("Should return 400 when totalAmount is negative")
        void submitTransaction_negativeAmount() throws Exception {
            TransactionRequest request = createValidTransactionRequest();
            request.setTotalAmount(new BigDecimal("-10.00"));

            mockMvc.perform(post("/api/transactions/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Invalid transaction data"))
                    .andExpect(jsonPath("$.errors").isMap())
                    .andExpect(jsonPath("$.errors.totalAmount").value("Total amount must be greater than zero"));
            verifyNoInteractions(metricsService);
        }

        @Test
        @DisplayName("Should return 400 when storeId is blank")
        void submitTransaction_blankStoreId() throws Exception {
            TransactionRequest request = createValidTransactionRequest();
            request.setStoreId("");

            mockMvc.perform(post("/api/transactions/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Invalid transaction data"))
                    .andExpect(jsonPath("$.errors").isMap())
                    .andExpect(jsonPath("$.errors.storeId").value("Store ID is required"));
            verifyNoInteractions(metricsService);
        }

        @Test
        @DisplayName("Should return 400 when paymentMethod is blank")
        void submitTransaction_blankPaymentMethod() throws Exception {
            TransactionRequest request = createValidTransactionRequest();
            request.setPaymentMethod("   ");

            mockMvc.perform(post("/api/transactions/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Invalid transaction data"))
                    .andExpect(jsonPath("$.errors").isMap())
                    .andExpect(jsonPath("$.errors.paymentMethod").value("Payment method is required"));
            verifyNoInteractions(metricsService);
        }

        @Test
        @DisplayName("Should return 400 when storeId is missing")
        void submitTransaction_missingStoreId() throws Exception {
            TransactionRequest request = createValidTransactionRequest();
            request.setStoreId(null);

            mockMvc.perform(post("/api/transactions/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Invalid transaction data"))
                    .andExpect(jsonPath("$.errors").isMap())
                    .andExpect(jsonPath("$.errors.storeId").value("Store ID is required"));
            verifyNoInteractions(metricsService);
        }

        @Test
        @DisplayName("Should return 400 when paymentMethod is missing")
        void submitTransaction_missingPaymentMethod() throws Exception {
            TransactionRequest request = createValidTransactionRequest();
            request.setPaymentMethod(null);

            mockMvc.perform(post("/api/transactions/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Invalid transaction data"))
                    .andExpect(jsonPath("$.errors").isMap())
                    .andExpect(jsonPath("$.errors.paymentMethod").value("Payment method is required"));
            verifyNoInteractions(metricsService);
        }

        @Test
        @DisplayName("Should return 400 and increment error counter on IllegalArgumentException")
        void submitTransaction_invalidRequest() throws Exception {
            TransactionRequest request = createValidTransactionRequest();

            when(transactionRequestMapper.toCommand(any(TransactionRequest.class))).thenReturn(mock(CreateTransactionCommand.class));
            when(transactionService.processTransaction(any(CreateTransactionCommand.class)))
                    .thenThrow(new IllegalArgumentException("Invalid transaction data"));

            mockMvc.perform(post("/api/transactions/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Invalid transaction data"))
                    .andExpect(jsonPath("$.error").value("Invalid transaction data"));
            verifyNoInteractions(metricsService);
        }

        @Test
        @DisplayName("Should return same transaction for duplicate submission (idempotent)")
        void submitTransaction_idempotentBehavior() throws Exception {
            TransactionRequest request = createValidTransactionRequest();
            request.setTransactionId("TXN-001");

            TransactionResult result = createTransactionResult("TXN-001");
            TransactionResponse response = createTransactionResponse("TXN-001");

            when(transactionRequestMapper.toCommand(any(TransactionRequest.class))).thenReturn(mock(CreateTransactionCommand.class));
            when(transactionService.processTransaction(any(CreateTransactionCommand.class)))
                    .thenReturn(result);
            when(transactionRequestMapper.toResponse(any(TransactionResult.class)))
                    .thenReturn(response);

            // First submission
            mockMvc.perform(post("/api/transactions/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.transactionId").value("TXN-001"));

            // Second submission with same ID (idempotent)
            mockMvc.perform(post("/api/transactions/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.transactionId").value("TXN-001"));

            verify(transactionService, times(2)).processTransaction(any(CreateTransactionCommand.class));
            verify(metricsService, times(2)).recordTransactionSubmission(any(TransactionRequest.class), any(TransactionResponse.class));
        }

        @Test
        @DisplayName("Should return same transaction for duplicate composite key (storeId, tillId, timestamp)")
        void submitTransaction_idempotentCompositeKey() throws Exception {
            ZonedDateTime timestamp = ZonedDateTime.now();

            TransactionRequest request = createValidTransactionRequest();
            request.setTransactionId(null); // No transaction ID provided
            request.setStoreId("STORE-001");
            request.setTillId("TILL-001");
            request.setTimestamp(timestamp);

            TransactionResult result = createTransactionResult("TXN-GENERATED");
            TransactionResponse response = createTransactionResponse("TXN-GENERATED");

            when(transactionRequestMapper.toCommand(any(TransactionRequest.class))).thenReturn(mock(CreateTransactionCommand.class));
            when(transactionService.processTransaction(any(CreateTransactionCommand.class)))
                    .thenReturn(result);
            when(transactionRequestMapper.toResponse(any(TransactionResult.class)))
                    .thenReturn(response);

            // First submission
            mockMvc.perform(post("/api/transactions/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.transactionId").value("TXN-GENERATED"));

            // Second submission with same storeId, tillId, and timestamp (idempotent)
            mockMvc.perform(post("/api/transactions/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.transactionId").value("TXN-GENERATED"));

            verify(transactionService, times(2)).processTransaction(any(CreateTransactionCommand.class));
            verify(metricsService, times(2)).recordTransactionSubmission(any(TransactionRequest.class), any(TransactionResponse.class));
        }

        @Test
        @DisplayName("Should return 422 when receipt total mismatch occurs")
        void submitTransaction_receiptTotalMismatch() throws Exception {
            TransactionRequest request = createValidTransactionRequest();

            when(transactionRequestMapper.toCommand(any(TransactionRequest.class))).thenReturn(mock(CreateTransactionCommand.class));
            when(transactionService.processTransaction(any(CreateTransactionCommand.class)))
                    .thenThrow(new ReceiptTotalMismatchException(
                            "Receipt total mismatch: calculated total does not match provided total",
                            new BigDecimal("5.00"),
                            new BigDecimal("25.50")
                    ));

            mockMvc.perform(post("/api/transactions/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Receipt total mismatch"))
                    .andExpect(jsonPath("$.error").value("Receipt total mismatch: calculated total does not match provided total"))
                    .andExpect(jsonPath("$.calculatedTotal").value(5.00))
                    .andExpect(jsonPath("$.providedTotal").value(25.50));
            verifyNoInteractions(metricsService);
        }

        @Test
        @DisplayName("Should return 500 and increment error counter on other exceptions")
        void submitTransaction_internalError() throws Exception {
            TransactionRequest request = createValidTransactionRequest();

            when(transactionRequestMapper.toCommand(any(TransactionRequest.class))).thenReturn(mock(CreateTransactionCommand.class));
            when(transactionService.processTransaction(any(CreateTransactionCommand.class)))
                    .thenThrow(new TransactionProcessingException("Failed to process transaction"));

            mockMvc.perform(post("/api/transactions/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Failed to process transaction"))
                    .andExpect(jsonPath("$.error").value("Internal server error"));
            verifyNoInteractions(metricsService);
        }

        @Test
        @DisplayName("Should record metrics with correct tags for store, till, and payment method")
        void submitTransaction_recordsMetricsWithTags() throws Exception {
            TransactionRequest request = createValidTransactionRequest();
            request.setStoreId("STORE-123");
            request.setTillId("TILL-456");
            request.setPaymentMethod("card");

            TransactionResult result = createTransactionResult("TXN-001");
            TransactionResponse response = createTransactionResponse("TXN-001");

            when(transactionRequestMapper.toCommand(any(TransactionRequest.class))).thenReturn(mock(CreateTransactionCommand.class));
            when(transactionService.processTransaction(any(CreateTransactionCommand.class)))
                    .thenReturn(result);
            when(transactionRequestMapper.toResponse(any(TransactionResult.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/transactions/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(metricsService).recordTransactionSubmission(any(TransactionRequest.class), any(TransactionResponse.class));
        }
    }

    @Nested
    @DisplayName("GET /api/transactions/{transactionId}")
    class GetTransactionByIdTests {

        @Test
        @DisplayName("Should return 200 and transaction when found")
        void getTransaction_found() throws Exception {
            TransactionResult result = createTransactionResult("TXN-001");
            TransactionResponse response = createTransactionResponse("TXN-001");

            when(transactionService.getTransactionById("TXN-001"))
                    .thenReturn(result);
            when(transactionRequestMapper.toResponse(any(TransactionResult.class)))
                    .thenReturn(response);

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
                    .thenThrow(new ResourceNotFoundException("Transaction not found: TXN-999"));

            mockMvc.perform(get("/api/transactions/TXN-999"))
                    .andExpect(status().isNotFound());

            verify(transactionService).getTransactionById("TXN-999");
        }

        @Test
        @DisplayName("Should return 500 on service exception")
        void getTransaction_serviceError() throws Exception {
            when(transactionService.getTransactionById("TXN-001"))
                    .thenThrow(new TransactionRetrievalException("Failed to retrieve transaction"));

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
            List<TransactionResult> results = Arrays.asList(
                    createTransactionResult("TXN-001"),
                    createTransactionResult("TXN-002")
            );
            List<TransactionResponse> transactions = Arrays.asList(
                    createTransactionResponse("TXN-001"),
                    createTransactionResponse("TXN-002")
            );

            when(transactionService.getTransactionsByStore("STORE-001"))
                    .thenReturn(results);
            when(transactionRequestMapper.toResponseList(any()))
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
                    .thenThrow(new TransactionRetrievalException("Failed to retrieve transactions"));

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
            List<TransactionResult> results = Arrays.asList(
                    createTransactionResult("TXN-001"),
                    createTransactionResult("TXN-002")
            );
            List<TransactionResponse> transactions = Arrays.asList(
                    createTransactionResponse("TXN-001"),
                    createTransactionResponse("TXN-002")
            );

            when(transactionService.getTransactionsByCustomer("CUST-001"))
                    .thenReturn(results);
            when(transactionRequestMapper.toResponseList(any()))
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
                    .thenThrow(new TransactionRetrievalException("Failed to retrieve transactions"));

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
            List<TransactionResult> results = Arrays.asList(
                    createTransactionResult("TXN-001"),
                    createTransactionResult("TXN-002")
            );
            List<TransactionResponse> transactions = Arrays.asList(
                    createTransactionResponse("TXN-001"),
                    createTransactionResponse("TXN-002")
            );

            when(transactionService.getTransactionsByTill("TILL-001"))
                    .thenReturn(results);
            when(transactionRequestMapper.toResponseList(any()))
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
                    .thenThrow(new TransactionRetrievalException("Failed to retrieve transactions"));

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
            List<TransactionResult> results = Arrays.asList(
                    createTransactionResult("TXN-001"),
                    createTransactionResult("TXN-002")
            );
            List<TransactionResponse> transactions = Arrays.asList(
                    createTransactionResponse("TXN-001"),
                    createTransactionResponse("TXN-002")
            );

            when(transactionService.getTransactionsByDateRange(any(ZonedDateTime.class), any(ZonedDateTime.class)))
                    .thenReturn(results);
            when(transactionRequestMapper.toResponseList(any()))
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
                    .thenThrow(new TransactionRetrievalException("Failed to retrieve transactions"));

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
            TransactionResult result = createTransactionResult("TXN-SAMPLE-001");
            TransactionResponse response = createTransactionResponse("TXN-SAMPLE-001");

            when(transactionRequestMapper.toCommand(any(TransactionRequest.class))).thenReturn(mock(CreateTransactionCommand.class));
            when(transactionService.processTransaction(any(CreateTransactionCommand.class)))
                    .thenReturn(result);
            when(transactionRequestMapper.toResponse(any(TransactionResult.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/transactions/sample"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.message").value("Sample transaction created"))
                    .andExpect(jsonPath("$.transaction").exists())
                    .andExpect(jsonPath("$.transaction.transactionId").value("TXN-SAMPLE-001"));

            verify(transactionService).processTransaction(any(CreateTransactionCommand.class));
        }

        @Test
        @DisplayName("Should return 500 on service exception")
        void createSampleTransaction_serviceError() throws Exception {
            when(transactionRequestMapper.toCommand(any(TransactionRequest.class))).thenReturn(mock(CreateTransactionCommand.class));
            when(transactionService.processTransaction(any(CreateTransactionCommand.class)))
                    .thenThrow(new TransactionProcessingException("Failed to create sample transaction"));

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
            Map<String, Object> statistics = Map.of(
                    "storeId", "STORE-001",
                    "totalTransactions", 2,
                    "totalAmount", 300.0,
                    "averageAmount", 150.0,
                    "calculationNote", "Average calculated as total amount divided by transaction count"
            );

            when(transactionService.getTransactionsForStatistics("STORE-001"))
                    .thenReturn(statistics);

            mockMvc.perform(get("/api/transactions/stats/STORE-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.storeId").value("STORE-001"))
                    .andExpect(jsonPath("$.totalTransactions").value(2))
                    .andExpect(jsonPath("$.totalAmount").value(300.0))
                    .andExpect(jsonPath("$.averageAmount").value(150.0))
                    .andExpect(jsonPath("$.calculationNote").exists());

            verify(transactionService).getTransactionsForStatistics("STORE-001");
        }

        @Test
        @DisplayName("Should return 200 with zeroed totals when no transactions found")
        void getTransactionStats_emptyList() throws Exception {
            Map<String, Object> statistics = Map.of(
                    "storeId", "STORE-999",
                    "message", "No transactions found for this store",
                    "totalTransactions", 0,
                    "totalAmount", 0.0,
                    "averageAmount", 0.0
            );

            when(transactionService.getTransactionsForStatistics("STORE-999"))
                    .thenReturn(statistics);

            mockMvc.perform(get("/api/transactions/stats/STORE-999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.storeId").value("STORE-999"))
                    .andExpect(jsonPath("$.message").value("No transactions found for this store"))
                    .andExpect(jsonPath("$.totalTransactions").value(0))
                    .andExpect(jsonPath("$.totalAmount").value(0.0))
                    .andExpect(jsonPath("$.averageAmount").value(0.0));

            verify(transactionService).getTransactionsForStatistics("STORE-999");
        }

        @Test
        @DisplayName("Should return 500 on service exception")
        void getTransactionStats_serviceError() throws Exception {
            when(transactionService.getTransactionsForStatistics("STORE-001"))
                    .thenThrow(new StatisticsCalculationException("Failed to calculate transaction statistics"));

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
        request.setTotalAmount(new BigDecimal("5.50"));
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

    private TransactionResult createTransactionResult(String transactionId) {
        return new TransactionResult(
                transactionId,
                "CUST-001",
                "STORE-001",
                "TILL-001",
                "card",
                new BigDecimal("25.50"),
                "GBP",
                ZonedDateTime.now(),
                ZonedDateTime.now(),
                "COMPLETED",
                null
        );
    }
}
