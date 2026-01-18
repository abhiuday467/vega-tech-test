package com.vega.techtest.controller;

import com.vega.techtest.aspect.Timed;
import com.vega.techtest.dto.TransactionItemRequest;
import com.vega.techtest.dto.TransactionRequest;
import com.vega.techtest.dto.TransactionResponse;
import com.vega.techtest.mapper.TransactionRequestMapper;
import com.vega.techtest.service.TransactionService;
import com.vega.techtest.service.TransactionMetricsService;
import com.vega.techtest.service.command.CreateTransactionCommand;
import com.vega.techtest.service.command.TransactionResult;
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
    private final TransactionRequestMapper transactionRequestMapper;

    @Timed("transaction_submission_duration")
    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitTransaction(@Valid @RequestBody TransactionRequest request) {
        logger.info("Received transaction submission from till: {}", request.getTillId());

        CreateTransactionCommand command = transactionRequestMapper.toCommand(request);
        TransactionResult result = transactionService.processTransaction(command);

        TransactionResponse response = transactionRequestMapper.toResponse(result);
        metricsService.recordTransactionSubmission(request, response);

        logger.info("Successfully processed transaction: {}", result.transactionId());

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
        TransactionResult result = transactionService.getTransactionById(transactionId);
        TransactionResponse transaction = transactionRequestMapper.toResponse(result);
        metricsService.recordTransactionRetrieval();

        return ResponseEntity.ok(transaction);
    }

    @Timed("transaction_retrieval_duration")
    @GetMapping("/store/{storeId}")
    public ResponseEntity<Map<String, Object>> getTransactionsByStore(@PathVariable String storeId) {
        List<TransactionResult> results = transactionService.getTransactionsByStore(storeId);
        List<TransactionResponse> transactions = transactionRequestMapper.toResponseList(results);
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
        List<TransactionResult> results = transactionService.getTransactionsByCustomer(customerId);
        List<TransactionResponse> transactions = transactionRequestMapper.toResponseList(results);
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
        List<TransactionResult> results = transactionService.getTransactionsByTill(tillId);
        List<TransactionResponse> transactions = transactionRequestMapper.toResponseList(results);
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
        List<TransactionResult> results = transactionService.getTransactionsByDateRange(startDate, endDate);
        List<TransactionResponse> transactions = transactionRequestMapper.toResponseList(results);
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
        List<TransactionItemRequest> items = List.of(
                new TransactionItemRequest("Milk", "MILK001", new BigDecimal("2.50"), 1, "Dairy"),
                new TransactionItemRequest("Bread", "BREAD001", new BigDecimal("1.20"), 1, "Bakery"),
                new TransactionItemRequest("Coffee", "COFFEE001", new BigDecimal("3.99"), 1, "Beverages")
        );

        TransactionRequest request = new TransactionRequest(
                null,
                "CUST-" + (int) (Math.random() * 99999),
                "STORE-001",
                "TILL-" + (int) (Math.random() * 10 + 1),
                Math.random() > 0.5 ? "card" : "cash",
                new BigDecimal("7.69"),
                "GBP",
                ZonedDateTime.now(),
                items
        );

        CreateTransactionCommand command = transactionRequestMapper.toCommand(request);
        TransactionResult result = transactionService.processTransaction(command);

        TransactionResponse response = transactionRequestMapper.toResponse(result);
        metricsService.recordTransactionSubmission(request, response);

        logger.info("Created sample transaction: {}", result.transactionId());

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
