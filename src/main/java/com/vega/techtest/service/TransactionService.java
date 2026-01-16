package com.vega.techtest.service;

import com.vega.techtest.dto.TransactionItemResponse;
import com.vega.techtest.dto.TransactionRequest;
import com.vega.techtest.dto.TransactionResponse;
import com.vega.techtest.entity.TransactionEntity;
import com.vega.techtest.entity.TransactionItemEntity;
import com.vega.techtest.exception.ResourceNotFoundException;
import com.vega.techtest.exception.StatisticsCalculationException;
import com.vega.techtest.exception.TransactionProcessingException;
import com.vega.techtest.exception.TransactionRetrievalException;
import com.vega.techtest.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;

    @Autowired
    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public TransactionResponse processTransaction(TransactionRequest request) {
        try {
            logger.info("Processing transaction request: {}", request.getTransactionId());

            String transactionId = request.getTransactionId();
            if (transactionId == null || transactionId.trim().isEmpty()) {
                transactionId = generateTransactionId();
                request.setTransactionId(transactionId);
            }

            if (transactionRepository.existsByTransactionId(transactionId)) {
                throw new IllegalArgumentException("Transaction ID already exists: " + transactionId);
            }

            if (request.getTimestamp() == null) {
                request.setTimestamp(ZonedDateTime.now());
            }

            validateTransactionRequest(request);

            TransactionEntity transaction = new TransactionEntity(
                    transactionId,
                    request.getCustomerId(),
                    request.getStoreId(),
                    request.getTillId(),
                    request.getPaymentMethod(),
                    request.getTotalAmount()
            );

            transaction.setCurrency(request.getCurrency());
            transaction.setTransactionTimestamp(request.getTimestamp());

            if (request.getItems() != null && !request.getItems().isEmpty()) {
                List<TransactionItemEntity> items = request.getItems().stream()
                        .map(itemRequest -> new TransactionItemEntity(
                                transaction,
                                itemRequest.getProductName(),
                                itemRequest.getProductCode(),
                                itemRequest.getUnitPrice(),
                                itemRequest.getQuantity(),
                                itemRequest.getCategory()
                        ))
                        .collect(Collectors.toList());

                transaction.setItems(items);
            }

            TransactionEntity savedTransaction = transactionRepository.save(transaction);
            logger.info("Successfully saved transaction: {}", transactionId);

            return convertToResponse(savedTransaction);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new TransactionProcessingException("Failed to process transaction", e);
        }
    }

    public TransactionResponse getTransactionById(String transactionId) {
        try {
            return transactionRepository.findByTransactionId(transactionId)
                    .map(this::convertToResponse)
                    .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new TransactionRetrievalException("Failed to retrieve transaction", e);
        }
    }

    public List<TransactionResponse> getTransactionsByStore(String storeId) {
        try {
            return loadTransactionsByStore(storeId);
        } catch (Exception e) {
            throw new TransactionRetrievalException("Failed to retrieve transactions", e);
        }
    }

    public List<TransactionResponse> getTransactionsByCustomer(String customerId) {
        try {
            return transactionRepository.findByCustomerIdOrderByTransactionTimestampDesc(customerId)
                    .stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new TransactionRetrievalException("Failed to retrieve transactions", e);
        }
    }

    public List<TransactionResponse> getTransactionsByTill(String tillId) {
        try {
            return transactionRepository.findByTillIdOrderByTransactionTimestampDesc(tillId)
                    .stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new TransactionRetrievalException("Failed to retrieve transactions", e);
        }
    }

    public List<TransactionResponse> getTransactionsByDateRange(ZonedDateTime startDate, ZonedDateTime endDate) {
        try {
            return transactionRepository.findTransactionsByDateRange(startDate, endDate)
                    .stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new TransactionRetrievalException("Failed to retrieve transactions", e);
        }
    }

    public List<TransactionResponse> getTransactionsForStatistics(String storeId) {
        try {
            return loadTransactionsByStore(storeId);
        } catch (Exception e) {
            throw new StatisticsCalculationException("Failed to calculate transaction statistics", e);
        }
    }

    private List<TransactionResponse> loadTransactionsByStore(String storeId) {
        return transactionRepository.findByStoreIdOrderByTransactionTimestampDesc(storeId)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private String generateTransactionId() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void validateTransactionRequest(TransactionRequest request) {
        if (request.getStoreId() == null || request.getStoreId().trim().isEmpty()) {
            throw new IllegalArgumentException("Store ID is required");
        }

        if (request.getPaymentMethod() == null || request.getPaymentMethod().trim().isEmpty()) {
            throw new IllegalArgumentException("Payment method is required");
        }

        if (request.getTotalAmount() == null || request.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Total amount must be greater than zero");
        }

        if (request.getItems() != null) {
            BigDecimal calculatedTotal = request.getItems().stream()
                    .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (calculatedTotal.compareTo(request.getTotalAmount()) != 0) {
                logger.warn("Calculated total ({}) doesn't match provided total ({})",
                        calculatedTotal, request.getTotalAmount());
            }
        }
    }

    private TransactionResponse convertToResponse(TransactionEntity entity) {
        TransactionResponse response = new TransactionResponse(
                entity.getTransactionId(),
                entity.getCustomerId(),
                entity.getStoreId(),
                entity.getTillId(),
                entity.getPaymentMethod(),
                entity.getTotalAmount(),
                entity.getCurrency(),
                entity.getTransactionTimestamp(),
                entity.getStatus()
        );

        response.setCreatedAt(entity.getCreatedAt());

        if (entity.getItems() != null && !entity.getItems().isEmpty()) {
            List<TransactionItemResponse> itemResponses = entity.getItems().stream()
                    .map(item -> new TransactionItemResponse(
                            item.getProductName(),
                            item.getProductCode(),
                            item.getUnitPrice(),
                            item.getQuantity(),
                            item.getTotalPrice(),
                            item.getCategory()
                    ))
                    .collect(Collectors.toList());

            response.setItems(itemResponses);
        }

        return response;
    }
}
