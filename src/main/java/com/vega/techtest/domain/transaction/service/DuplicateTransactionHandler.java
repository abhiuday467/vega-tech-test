package com.vega.techtest.domain.transaction.service;

import com.vega.techtest.domain.transaction.model.TransactionItem;
import com.vega.techtest.adapter.out.persistence.entity.TransactionEntity;
import com.vega.techtest.adapter.out.persistence.entity.TransactionItemEntity;
import com.vega.techtest.mapper.TransactionEntityMapper;
import com.vega.techtest.adapter.out.persistence.repository.TransactionRepository;
import com.vega.techtest.application.transaction.command.CreateTransactionCommand;
import com.vega.techtest.application.transaction.command.TransactionResult;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Service
public class DuplicateTransactionHandler {

    private static final Logger logger = LoggerFactory.getLogger(DuplicateTransactionHandler.class);

    private final TransactionRepository transactionRepository;
    private final TransactionEntityMapper mapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public TransactionResult findExistingTransaction(CreateTransactionCommand command) {
        TransactionEntity existingTransaction = transactionRepository
                .findByStoreIdAndTillIdAndTransactionTimestamp(
                        command.storeId(),
                        command.tillId(),
                        command.timestamp()
                );

        // If null, the constraint violation was on transaction_id, not (store, till, timestamp)
        if (existingTransaction == null) {
            String message = String.format(
                    "Database constraint violation occurred but no transaction found with StoreId=%s, TillId=%s, Timestamp=%s. " +
                    "This indicates a duplicate transaction_id (%s) with different business data. " +
                    "Possible causes: (1) Same transactionId sent from different stores/tills, " +
                    "(2) Race condition where transaction not yet committed, " +
                    "(3) Transaction rolled back after constraint check.",
                    command.storeId(),
                    command.tillId(),
                    command.timestamp(),
                    command.transactionId()
            );
            logger.error(message);
            throw new IllegalStateException(message);
        }

        List<String> differences = findDifferences(command, existingTransaction);
        if (!differences.isEmpty()) {
            String message = String.format(
                    "URGENT- Bad Transactions coming from StoreId %s TillID %s for the same timestamp, " +
                            "two different transaction reported! Existing vs Sent: %s",
                    command.storeId(),
                    command.tillId(),
                    String.join("; ", differences)
            );
            logger.error(message);
            throw new IllegalStateException(message);
        }

        logger.warn("Duplicate transaction detected - Timestamp: {}, StoreId: {}, TillId: {}. " +
                        "Returning existing transaction: {}",
                command.timestamp(), command.storeId(), command.tillId(),
                existingTransaction.getTransactionId());

        return mapper.toResult(existingTransaction);
    }

    private List<String> findDifferences(CreateTransactionCommand command, TransactionEntity existing) {
        List<String> differences = new ArrayList<>();

        if (command.transactionId() != null && !command.transactionId().isBlank()) {
            addIfDifferent("transactionId", command.transactionId(), existing.getTransactionId(), differences);
        }

        addIfDifferent("customerId", command.customerId(), existing.getCustomerId(), differences);
        addIfDifferent("storeId", command.storeId(), existing.getStoreId(), differences);
        addIfDifferent("tillId", command.tillId(), existing.getTillId(), differences);
        addIfDifferent("paymentMethod", command.paymentMethod(), existing.getPaymentMethod(), differences);
        addIfDifferent("currency", command.currency(), existing.getCurrency(), differences);
        addIfDifferentBigDecimal("totalAmount", command.totalAmount(), existing.getTotalAmount(), differences);
        addIfDifferentTimestamp("timestamp", command.timestamp(), existing.getTransactionTimestamp(), differences);

        List<TransactionItemEntity> existingItems =
                existing.getItems() == null ? Collections.emptyList() : existing.getItems();
        List<TransactionItem> commandItems =
                command.items() == null ? Collections.emptyList() : command.items();

        if (existingItems.size() != commandItems.size()) {
            differences.add(String.format("items.size existing=%d sent=%d", existingItems.size(), commandItems.size()));
            return differences;
        }

        for (int i = 0; i < commandItems.size(); i++) {
            var sent = commandItems.get(i);
            var existingItem = existingItems.get(i);
            addIfDifferent(String.format("items[%d].productName", i), sent.productName(), existingItem.getProductName(), differences);
            addIfDifferent(String.format("items[%d].productCode", i), sent.productCode(), existingItem.getProductCode(), differences);
            addIfDifferentBigDecimal(String.format("items[%d].unitPrice", i), sent.unitPrice(), existingItem.getUnitPrice(), differences);
            addIfDifferent(String.format("items[%d].quantity", i), sent.quantity(), existingItem.getQuantity(), differences);
            addIfDifferent(String.format("items[%d].category", i), sent.category(), existingItem.getCategory(), differences);
        }

        return differences;
    }

    private void addIfDifferent(String field, Object sent, Object existing, List<String> differences) {
        if (!Objects.equals(sent, existing)) {
            differences.add(String.format("%s existing=%s sent=%s", field, existing, sent));
        }
    }

    private void addIfDifferentBigDecimal(String field, BigDecimal sent, BigDecimal existing, List<String> differences) {
        if (sent == null && existing == null) {
            return;
        }
        if (sent == null || existing == null || sent.compareTo(existing) != 0) {
            differences.add(String.format("%s existing=%s sent=%s", field, existing, sent));
        }
    }

    private void addIfDifferentTimestamp(String field, Instant sent, Instant existing, List<String> differences) {
        if (sent == null && existing == null) {
            return;
        }
        if (sent == null || existing == null || !sent.equals(existing)) {
            differences.add(String.format("%s existing=%s sent=%s", field, existing, sent));
        }
    }
}
