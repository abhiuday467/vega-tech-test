package com.vega.techtest.domain.transaction.service;

import com.vega.techtest.domain.transaction.exception.*;
import com.vega.techtest.adapter.out.persistence.entity.TransactionEntity;
import com.vega.techtest.adapter.out.persistence.entity.TransactionItemEntity;
import com.vega.techtest.mapper.TransactionEntityMapper;
import com.vega.techtest.adapter.out.persistence.repository.TransactionRepository;
import com.vega.techtest.application.transaction.command.CreateTransactionCommand;
import com.vega.techtest.application.transaction.command.TransactionResult;
import com.vega.techtest.domain.transaction.validator.TransactionValidator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.vega.techtest.shared.util.Calculator.calculateAverageAmount;

@RequiredArgsConstructor
@Service
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final TransactionValidator validator;
    private final TransactionEntityMapper mapper;
    private final DuplicateTransactionHandler duplicateTransactionHandler;

    public TransactionResult processTransaction(CreateTransactionCommand command) {
        try {
            logger.info("Processing transaction from store: {}, till: {}, at: {}",
                command.storeId(), command.tillId(), command.timestamp());
            validator.validateTransactionCommand(command);

            return createTransaction(command);
        } catch (DataIntegrityViolationException e) {
           return duplicateTransactionHandler.findExistingTransaction(command);
        } catch(ReceiptTotalMismatchException | IllegalStateException | IllegalArgumentException e ){
            throw e;
        } catch (Exception e) {
            throw new TransactionProcessingException("Failed to process transaction", e);
        }
    }

    @Transactional
    protected TransactionResult createTransaction(CreateTransactionCommand command) {
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

    public List<TransactionResult> getTransactionsByDateRange(Instant startDate, Instant endDate) {
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
