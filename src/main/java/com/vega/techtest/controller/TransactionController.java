package com.vega.techtest.controller;

import com.vega.techtest.aspect.Timed;
import com.vega.techtest.dto.TransactionItemRequest;
import com.vega.techtest.dto.TransactionRequest;
import com.vega.techtest.dto.TransactionResponse;
import com.vega.techtest.service.TransactionService;
import com.vega.techtest.service.TransactionMetricsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.*;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    private final TransactionService transactionService;
    private final TransactionMetricsService metricsService;

    @Timed("transaction_submission_duration")
    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitTransaction(@Valid @RequestBody TransactionRequest request) {
        logger.info("Received transaction submission from till: {}", request.getTillId());

        TransactionResponse response = transactionService.processTransaction(request);
        metricsService.recordTransactionSubmission(request, response);

        logger.info("Successfully processed transaction: {}", response.getTransactionId());

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Transaction processed successfully",
                "transactionId", response.getTransactionId(),
                "timestamp", response.getTransactionTimestamp()
        ));
    }

    @Timed("transaction_retrieval_duration")
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable String transactionId) {
        TransactionResponse transaction = transactionService.getTransactionById(transactionId);
        metricsService.recordTransactionRetrieval();

        return ResponseEntity.ok(transaction);
    }

    @Timed("transaction_retrieval_duration")
    @GetMapping("/store/{storeId}")
    public ResponseEntity<Map<String, Object>> getTransactionsByStore(@PathVariable String storeId) {
        List<TransactionResponse> transactions = transactionService.getTransactionsByStore(storeId);
        metricsService.recordTransactionRetrieval();

        return ResponseEntity.ok(Map.of(
                "storeId", storeId,
                "count", transactions.size(),
                "transactions", transactions
        ));
    }

    @Timed("transaction_retrieval_duration")
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<Map<String, Object>> getTransactionsByCustomer(@PathVariable String customerId) {
        List<TransactionResponse> transactions = transactionService.getTransactionsByCustomer(customerId);
        metricsService.recordTransactionRetrieval();

        return ResponseEntity.ok(Map.of(
                "customerId", customerId,
                "count", transactions.size(),
                "transactions", transactions
        ));
    }

    @Timed("transaction_retrieval_duration")
    @GetMapping("/till/{tillId}")
    public ResponseEntity<Map<String, Object>> getTransactionsByTill(@PathVariable String tillId) {
        List<TransactionResponse> transactions = transactionService.getTransactionsByTill(tillId);
        metricsService.recordTransactionRetrieval();

        return ResponseEntity.ok(Map.of(
                "tillId", tillId,
                "count", transactions.size(),
                "transactions", transactions
        ));
    }

    @Timed("transaction_retrieval_duration")
    @GetMapping("/date-range")
    public ResponseEntity<Map<String, Object>> getTransactionsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate) {
        List<TransactionResponse> transactions = transactionService.getTransactionsByDateRange(startDate, endDate);
        metricsService.recordTransactionRetrieval();

        return ResponseEntity.ok(Map.of(
                "startDate", startDate,
                "endDate", endDate,
                "count", transactions.size(),
                "transactions", transactions
        ));
    }

    // This endpoint should not go to production
    @Profile("!prod")
    @Timed("transaction_submission_duration")
    @PostMapping("/sample")
    public ResponseEntity<Map<String, Object>> createSampleTransaction() {
        TransactionRequest request = new TransactionRequest();
        request.setCustomerId("CUST-" + (int) (Math.random() * 99999));
        request.setStoreId("STORE-001");
        request.setTillId("TILL-" + (int) (Math.random() * 10 + 1));
        request.setPaymentMethod(Math.random() > 0.5 ? "card" : "cash");
        request.setTotalAmount(new BigDecimal("7.69"));
        request.setCurrency("GBP");
        request.setTimestamp(ZonedDateTime.now());

        request.setItems(List.of(
                new TransactionItemRequest("Milk", "MILK001", new BigDecimal("2.50"), 1, "Dairy"),
                new TransactionItemRequest("Bread", "BREAD001", new BigDecimal("1.20"), 1, "Bakery"),
                new TransactionItemRequest("Coffee", "COFFEE001", new BigDecimal("3.99"), 1, "Beverages")
        ));

        TransactionResponse response = transactionService.processTransaction(request);
        metricsService.recordTransactionSubmission(request, response);

        logger.info("Created sample transaction: {}", response.getTransactionId());

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Sample transaction created",
                "transaction", response
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "transaction-service",
                "timestamp", String.valueOf(System.currentTimeMillis())
        ));
    }

    @Timed("transaction_retrieval_duration")
    @GetMapping("/stats/{storeId}")
    public ResponseEntity<Map<String, Object>> getTransactionStats(@PathVariable String storeId) {
        Map<String, Object> statistics = transactionService.getTransactionsForStatistics(storeId);
        metricsService.recordTransactionRetrieval();
        return ResponseEntity.ok(statistics);
    }
}
