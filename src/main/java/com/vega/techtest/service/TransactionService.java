package com.vega.techtest.service;

import com.vega.techtest.dto.TransactionRequest;
import com.vega.techtest.dto.TransactionResponse;
import com.vega.techtest.entity.TransactionEntity;
import com.vega.techtest.entity.TransactionItemEntity;
import com.vega.techtest.exception.ResourceNotFoundException;
import com.vega.techtest.exception.StatisticsCalculationException;
import com.vega.techtest.exception.TransactionProcessingException;
import com.vega.techtest.exception.TransactionRetrievalException;
import com.vega.techtest.mapper.TransactionEntityMapper;
import com.vega.techtest.repository.TransactionRepository;
import com.vega.techtest.validators.TransactionValidator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;

import static com.vega.techtest.utils.Calculator.calculateAverageAmount;

@RequiredArgsConstructor
@Service
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final TransactionValidator validator;
    private final TransactionEntityMapper mapper;

    @Transactional
    public TransactionResponse processTransaction(TransactionRequest request) {
        try {
            logger.info("Processing transaction request: {}", request.getTransactionId());
            validator.validateTransactionRequest(request);

            String transactionId = request.getTransactionId();
            if (transactionId == null || transactionId.trim().isEmpty()) {
                transactionId = generateTransactionId();
                request.setTransactionId(transactionId);
            }
            TransactionEntity transaction = mapper.toEntity(request);
            transaction.setTransactionId(transactionId);

            if (request.getItems() != null && !request.getItems().isEmpty()) {
                List<TransactionItemEntity> items = mapper.toItemEntityList(request.getItems());
                items.forEach(item -> item.setTransaction(transaction));
                transaction.setItems(items);
            }

            TransactionEntity savedTransaction = transactionRepository.save(transaction);
            logger.info("Successfully saved transaction: {}", transactionId);

            return mapper.toResponse(savedTransaction);
        } catch (DuplicateKeyException e) {
           return  getDuplicateTransaction(request);
        } catch (Exception e) {
            throw new TransactionProcessingException("Failed to process transaction", e);
        }
    }

    private TransactionResponse getDuplicateTransaction(TransactionRequest request) {
        TransactionEntity existingTransaction = transactionRepository
                .findByStoreIdAndTillIdAndTransactionTimestamp(
                        request.getStoreId(),
                        request.getTillId(),
                        request.getTimestamp()
                );
            logger.warn("Duplicate transaction detected - Timestamp: {}, StoreId: {}, TillId: {}. " +
                            "Returning existing transaction: {}",
                    request.getTimestamp(), request.getStoreId(), request.getTillId(),
                    existingTransaction.getTransactionId());
            return mapper.toResponse(existingTransaction);
    }

    public TransactionResponse getTransactionById(String transactionId) {
        try {
            return transactionRepository.findByTransactionId(transactionId)
                    .map(mapper::toResponse)
                    .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));
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
            return mapper.toResponseList(
                    transactionRepository.findByCustomerIdOrderByTransactionTimestampDesc(customerId)
            );
        } catch (Exception e) {
            throw new TransactionRetrievalException("Failed to retrieve transactions", e);
        }
    }

    public List<TransactionResponse> getTransactionsByTill(String tillId) {
        try {
            return mapper.toResponseList(
                    transactionRepository.findByTillIdOrderByTransactionTimestampDesc(tillId)
            );
        } catch (Exception e) {
            throw new TransactionRetrievalException("Failed to retrieve transactions", e);
        }
    }

    public List<TransactionResponse> getTransactionsByDateRange(ZonedDateTime startDate, ZonedDateTime endDate) {
        try {
            return mapper.toResponseList(
                    transactionRepository.findTransactionsByDateRange(startDate, endDate)
            );
        } catch (Exception e) {
            throw new TransactionRetrievalException("Failed to retrieve transactions", e);
        }
    }

    public Map<String, Object> getTransactionsForStatistics(String storeId) {
        try {
            logger.info("Calculating transaction statistics for store: {}", storeId);

            List<TransactionResponse> transactions = loadTransactionsByStore(storeId);

            if (transactions.isEmpty()) {
                logger.warn("No transactions found for store: {}", storeId);
                return Map.of(
                        "storeId", storeId,
                        "message", "No transactions found for this store",
                        "totalTransactions", 0,
                        "totalAmount", 0.0,
                        "averageAmount", 0.0
                );
            }

            int totalTransactions = transactions.size();
            BigDecimal totalAmount = transactions.stream()
                    .map(TransactionResponse::getTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal averageAmount = calculateAverageAmount(totalAmount, totalTransactions);

            logger.info("Store {} statistics - Total transactions: {}, Total amount: {}, Average amount: {}",
                    storeId, totalTransactions, totalAmount, averageAmount);

            return Map.of(
                    "storeId", storeId,
                    "totalTransactions", totalTransactions,
                    "totalAmount", totalAmount.doubleValue(),
                    "averageAmount", averageAmount.doubleValue(),
                    "calculationNote", "Average calculated as total amount divided by transaction count"
            );
        } catch (Exception e) {
            throw new StatisticsCalculationException("Failed to calculate transaction statistics", e);
        }
    }

    private List<TransactionResponse> loadTransactionsByStore(String storeId) {
        return mapper.toResponseList(
                transactionRepository.findByStoreIdOrderByTransactionTimestampDesc(storeId)
        );
    }

    private String generateTransactionId() {
        return "TXN-" + UUID.randomUUID().toString().toUpperCase();
    }
}
