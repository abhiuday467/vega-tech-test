package com.vega.techtest.service;

import com.vega.techtest.entity.TransactionEntity;
import com.vega.techtest.entity.TransactionItemEntity;
import com.vega.techtest.exception.ResourceNotFoundException;
import com.vega.techtest.exception.StatisticsCalculationException;
import com.vega.techtest.exception.TransactionProcessingException;
import com.vega.techtest.exception.TransactionRetrievalException;
import com.vega.techtest.mapper.TransactionEntityMapper;
import com.vega.techtest.repository.TransactionRepository;
import com.vega.techtest.service.command.CreateTransactionCommand;
import com.vega.techtest.service.command.TransactionResult;
import com.vega.techtest.validators.TransactionValidator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.vega.techtest.utils.Calculator.calculateAverageAmount;

@RequiredArgsConstructor
@Service
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final TransactionValidator validator;
    private final TransactionEntityMapper mapper;

    @Transactional
    public TransactionResult processTransaction(CreateTransactionCommand command) {
        try {
            logger.info("Processing transaction from store: {}, till: {}, at: {}",
                command.storeId(), command.tillId(), command.timestamp());
            validator.validateTransactionCommand(command);

            String transactionId = command.transactionId();
            if (transactionId == null || transactionId.trim().isEmpty()) {
                transactionId = generateTransactionId();
            }
            TransactionEntity transaction = mapper.toEntityFromCommand(command);
            transaction.setTransactionId(transactionId);

            if (command.items() != null && !command.items().isEmpty()) {
                List<TransactionItemEntity> items = mapper.toItemEntityListFromCommand(command.items());
                items.forEach(item -> item.setTransaction(transaction));
                transaction.setItems(items);
            }

            TransactionEntity savedTransaction = transactionRepository.save(transaction);
            logger.info("Successfully saved transaction: {}", transactionId);

            return mapper.toResult(savedTransaction);
        } catch (DuplicateKeyException e) {
           return getDuplicateTransaction(command);
        } catch (Exception e) {
            throw new TransactionProcessingException("Failed to process transaction", e);
        }
    }

    private TransactionResult getDuplicateTransaction(CreateTransactionCommand command) {
        TransactionEntity existingTransaction = transactionRepository
                .findByStoreIdAndTillIdAndTransactionTimestamp(
                        command.storeId(),
                        command.tillId(),
                        command.timestamp()
                );
            logger.warn("Duplicate transaction detected - Timestamp: {}, StoreId: {}, TillId: {}. " +
                            "Returning existing transaction: {}",
                    command.timestamp(), command.storeId(), command.tillId(),
                    existingTransaction.getTransactionId());
            return mapper.toResult(existingTransaction);
    }

    public TransactionResult getTransactionById(String transactionId) {
        try {
            return transactionRepository.findByTransactionId(transactionId)
                    .map(mapper::toResult)
                    .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));
        } catch (Exception e) {
            throw new TransactionRetrievalException("Failed to retrieve transaction", e);
        }
    }

    public List<TransactionResult> getTransactionsByStore(String storeId) {
        try {
            return loadTransactionsByStore(storeId);
        } catch (Exception e) {
            throw new TransactionRetrievalException("Failed to retrieve transactions", e);
        }
    }

    public List<TransactionResult> getTransactionsByCustomer(String customerId) {
        try {
            return mapper.toResultList(
                    transactionRepository.findByCustomerIdOrderByTransactionTimestampDesc(customerId)
            );
        } catch (Exception e) {
            throw new TransactionRetrievalException("Failed to retrieve transactions", e);
        }
    }

    public List<TransactionResult> getTransactionsByTill(String tillId) {
        try {
            return mapper.toResultList(
                    transactionRepository.findByTillIdOrderByTransactionTimestampDesc(tillId)
            );
        } catch (Exception e) {
            throw new TransactionRetrievalException("Failed to retrieve transactions", e);
        }
    }

    public List<TransactionResult> getTransactionsByDateRange(ZonedDateTime startDate, ZonedDateTime endDate) {
        try {
            return mapper.toResultList(
                    transactionRepository.findTransactionsByDateRange(startDate, endDate)
            );
        } catch (Exception e) {
            throw new TransactionRetrievalException("Failed to retrieve transactions", e);
        }
    }

    public Map<String, Object> getTransactionsForStatistics(String storeId) {
        try {
            logger.info("Calculating transaction statistics for store: {}", storeId);

            List<TransactionResult> transactions = loadTransactionsByStore(storeId);

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
                    .map(TransactionResult::totalAmount)
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

    private List<TransactionResult> loadTransactionsByStore(String storeId) {
        return mapper.toResultList(
                transactionRepository.findByStoreIdOrderByTransactionTimestampDesc(storeId)
        );
    }

    private String generateTransactionId() {
        return "TXN-" + UUID.randomUUID().toString().toUpperCase();
    }
}
