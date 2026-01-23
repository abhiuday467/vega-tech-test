package com.vega.techtest.adapter.in.rest.controller;

import com.vega.techtest.adapter.in.rest.dto.TransactionItemRequest;
import com.vega.techtest.adapter.in.rest.dto.TransactionRequest;
import com.vega.techtest.adapter.in.rest.dto.TransactionResponse;
import com.vega.techtest.adapter.in.rest.dto.ErrorResponse;
import com.vega.techtest.adapter.in.rest.dto.ReceiptTotalMismatchResponse;
import com.vega.techtest.adapter.in.rest.dto.ValidationErrorResponse;
import com.vega.techtest.adapter.in.rest.mapper.TransactionRequestMapper;
import com.vega.techtest.domain.transaction.service.TransactionService;
import com.vega.techtest.domain.transaction.service.TransactionMetricsService;
import com.vega.techtest.application.transaction.command.CreateTransactionCommand;
import com.vega.techtest.application.transaction.command.TransactionResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.vega.techtest.shared.aspect.Timed;

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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction processed successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Receipt total mismatch",
                    content = @Content(schema = @Schema(implementation = ReceiptTotalMismatchResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Map<String, Object>> submitTransaction(@Valid @RequestBody TransactionRequest request) {
        logger.info("Received transaction submission from till: {}", request.tillId());

        CreateTransactionCommand command = transactionRequestMapper.toCommand(request);
        TransactionResult result = transactionService.processTransaction(command);

        TransactionResponse response = transactionRequestMapper.toResponse(result);
        metricsService.recordTransactionSubmission(request, response);

        logger.info("Successfully processed transaction: {}", result.transactionId());

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Transaction processed successfully",
                "transactionId", response.transactionId(),
                "timestamp", response.transactionTimestamp()
        ));
    }

    @Timed("transaction_retrieval_duration")
    @GetMapping("/{transactionId}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction found"),
            @ApiResponse(responseCode = "404", description = "Transaction not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable String transactionId) {
        TransactionResult result = transactionService.getTransactionById(transactionId);
        TransactionResponse transaction = transactionRequestMapper.toResponse(result);
        metricsService.recordTransactionRetrieval();

        return ResponseEntity.ok(transaction);
    }

    @Timed("transaction_retrieval_duration")
    @GetMapping("/store/{storeId}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transactions found"),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transactions found"),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transactions found"),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transactions found"),
            @ApiResponse(responseCode = "400", description = "Invalid date range",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Map<String, Object>> getTransactionsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate) {
        List<TransactionResult> results = transactionService.getTransactionsByDateRange(
                startDate == null ? null : startDate.toInstant(),
                endDate == null ? null : endDate.toInstant()
        );
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sample transaction created"),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Service healthy")
    })
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "transaction-service",
                "timestamp", String.valueOf(System.currentTimeMillis())
        ));
    }

    @Timed("transaction_retrieval_duration")
    @GetMapping("/stats/{storeId}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statistics calculated"),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Map<String, Object>> getTransactionStats(@PathVariable String storeId) {
        Map<String, Object> statistics = transactionService.getTransactionsForStatistics(storeId);
        metricsService.recordTransactionRetrieval();
        return ResponseEntity.ok(statistics);
    }
}
