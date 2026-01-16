package com.vega.techtest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vega.techtest.dto.TransactionRequest;
import com.vega.techtest.dto.TransactionResponse;
import com.vega.techtest.entity.TransactionEntity;
import com.vega.techtest.exception.StatisticsCalculationException;
import com.vega.techtest.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    @DisplayName("Should generate transaction id and persist transaction")
    void processTransaction_generatesIdAndSaves() {
        TransactionRequest request = new TransactionRequest();
        request.setCustomerId("CUST-1");
        request.setStoreId("STORE-1");
        request.setTillId("TILL-1");
        request.setPaymentMethod("card");
        request.setTotalAmount(new BigDecimal("12.50"));

        when(transactionRepository.findByTransactionId(any())).thenReturn(Optional.empty());
        when(transactionRepository.save(any(TransactionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = transactionService.processTransaction(request);

        assertThat(response.getTransactionId()).startsWith("TXN-");
        assertThat(response.getStoreId()).isEqualTo("STORE-1");
        assertThat(response.getPaymentMethod()).isEqualTo("card");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("12.50");
        assertThat(response.getCurrency()).isEqualTo("GBP");

        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        verify(transactionRepository).findByTransactionId(idCaptor.capture());
        assertThat(idCaptor.getValue()).isEqualTo(response.getTransactionId());
    }

    @Test
    @DisplayName("Should return existing transaction for duplicate id (idempotent)")
    void processTransaction_returnsExistingForDuplicateId() {
        TransactionRequest request = new TransactionRequest();
        request.setTransactionId("TXN-EXIST");
        request.setStoreId("STORE-1");
        request.setTillId("TILL-1");
        request.setPaymentMethod("cash");
        request.setTotalAmount(new BigDecimal("9.99"));

        TransactionEntity existingEntity = new TransactionEntity(
                "TXN-EXIST",
                "CUST-1",
                "STORE-1",
                "TILL-1",
                "cash",
                new BigDecimal("9.99")
        );
        existingEntity.setCurrency("GBP");
        existingEntity.setTransactionTimestamp(ZonedDateTime.now());

        when(transactionRepository.findByTransactionId("TXN-EXIST")).thenReturn(Optional.of(existingEntity));

        TransactionResponse response = transactionService.processTransaction(request);

        assertThat(response.getTransactionId()).isEqualTo("TXN-EXIST");
        assertThat(response.getStoreId()).isEqualTo("STORE-1");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("9.99");

        verify(transactionRepository).findByTransactionId("TXN-EXIST");
        verify(transactionRepository, never()).save(any(TransactionEntity.class));
    }

    @Nested
    @DisplayName("getTransactionsForStatistics Tests")
    class GetTransactionsForStatisticsTests {

        @Test
        @DisplayName("Should calculate statistics correctly with multiple transactions")
        void getTransactionsForStatistics_calculatesCorrectly() {
            List<TransactionEntity> entities = List.of(
                    createTransactionEntity("TXN-001", "STORE-001", new BigDecimal("100.00")),
                    createTransactionEntity("TXN-002", "STORE-001", new BigDecimal("200.00")),
                    createTransactionEntity("TXN-003", "STORE-001", new BigDecimal("150.00"))
            );

            when(transactionRepository.findByStoreIdOrderByTransactionTimestampDesc("STORE-001"))
                    .thenReturn(entities);

            Map<String, Object> result = transactionService.getTransactionsForStatistics("STORE-001");

            assertThat(result.get("storeId")).isEqualTo("STORE-001");
            assertThat(result.get("totalTransactions")).isEqualTo(3);
            assertThat((Double) result.get("totalAmount")).isEqualTo(450.0);
            assertThat((Double) result.get("averageAmount")).isEqualTo(150.0);
            assertThat(result.get("calculationNote")).isEqualTo("Average calculated as total amount divided by transaction count");
            assertThat(result.containsKey("message")).isFalse();

            verify(transactionRepository).findByStoreIdOrderByTransactionTimestampDesc("STORE-001");
        }

        @Test
        @DisplayName("Should return zeroed statistics when no transactions found")
        void getTransactionsForStatistics_emptyList() {
            when(transactionRepository.findByStoreIdOrderByTransactionTimestampDesc("STORE-999"))
                    .thenReturn(Collections.emptyList());

            Map<String, Object> result = transactionService.getTransactionsForStatistics("STORE-999");

            assertThat(result.get("storeId")).isEqualTo("STORE-999");
            assertThat(result.get("message")).isEqualTo("No transactions found for this store");
            assertThat(result.get("totalTransactions")).isEqualTo(0);
            assertThat((Double) result.get("totalAmount")).isEqualTo(0.0);
            assertThat((Double) result.get("averageAmount")).isEqualTo(0.0);
            assertThat(result.containsKey("calculationNote")).isFalse();

            verify(transactionRepository).findByStoreIdOrderByTransactionTimestampDesc("STORE-999");
        }

        @Test
        @DisplayName("Should filter out null amounts when calculating totals")
        void getTransactionsForStatistics_filtersNullAmounts() {
            List<TransactionEntity> entities = new ArrayList<>();
            entities.add(createTransactionEntity("TXN-001", "STORE-001", new BigDecimal("100.00")));
            entities.add(createTransactionEntityWithNullAmount("TXN-002", "STORE-001"));
            entities.add(createTransactionEntity("TXN-003", "STORE-001", new BigDecimal("200.00")));

            when(transactionRepository.findByStoreIdOrderByTransactionTimestampDesc("STORE-001"))
                    .thenReturn(entities);

            Map<String, Object> result = transactionService.getTransactionsForStatistics("STORE-001");

            assertThat(result.get("storeId")).isEqualTo("STORE-001");
            assertThat(result.get("totalTransactions")).isEqualTo(3);
            assertThat((Double) result.get("totalAmount")).isEqualTo(300.0);
            assertThat((Double) result.get("averageAmount")).isEqualTo(100.0);
        }

        @Test
        @DisplayName("Should throw StatisticsCalculationException on repository error")
        void getTransactionsForStatistics_throwsExceptionOnError() {
            when(transactionRepository.findByStoreIdOrderByTransactionTimestampDesc("STORE-001"))
                    .thenThrow(new RuntimeException("Database error"));

            StatisticsCalculationException exception = assertThrows(
                    StatisticsCalculationException.class,
                    () -> transactionService.getTransactionsForStatistics("STORE-001")
            );

            assertThat(exception.getMessage()).isEqualTo("Failed to calculate transaction statistics");
            assertThat(exception.getCause()).isInstanceOf(RuntimeException.class);
            assertThat(exception.getCause().getMessage()).isEqualTo("Database error");
        }

        @Test
        @DisplayName("Should handle single transaction correctly")
        void getTransactionsForStatistics_singleTransaction() {
            List<TransactionEntity> entities = List.of(
                    createTransactionEntity("TXN-001", "STORE-001", new BigDecimal("99.99"))
            );

            when(transactionRepository.findByStoreIdOrderByTransactionTimestampDesc("STORE-001"))
                    .thenReturn(entities);

            Map<String, Object> result = transactionService.getTransactionsForStatistics("STORE-001");

            assertThat(result.get("totalTransactions")).isEqualTo(1);
            assertThat((Double) result.get("totalAmount")).isEqualTo(99.99);
            assertThat((Double) result.get("averageAmount")).isEqualTo(99.99);
        }

        @Test
        @DisplayName("Should handle large numbers with precision")
        void getTransactionsForStatistics_largeNumbersPrecision() {
            List<TransactionEntity> entities = List.of(
                    createTransactionEntity("TXN-001", "STORE-001", new BigDecimal("1000.50")),
                    createTransactionEntity("TXN-002", "STORE-001", new BigDecimal("2000.75")),
                    createTransactionEntity("TXN-003", "STORE-001", new BigDecimal("3000.25"))
            );

            when(transactionRepository.findByStoreIdOrderByTransactionTimestampDesc("STORE-001"))
                    .thenReturn(entities);

            Map<String, Object> result = transactionService.getTransactionsForStatistics("STORE-001");

            assertThat(result.get("totalTransactions")).isEqualTo(3);
            assertThat((Double) result.get("totalAmount")).isEqualTo(6001.50);
            assertThat((Double) result.get("averageAmount")).isEqualTo(2000.50);
        }
    }

    private TransactionEntity createTransactionEntity(String transactionId, String storeId, BigDecimal amount) {
        TransactionEntity entity = new TransactionEntity(
                transactionId,
                "CUST-001",
                storeId,
                "TILL-001",
                "card",
                amount
        );
        entity.setCurrency("GBP");
        entity.setTransactionTimestamp(ZonedDateTime.now());
        return entity;
    }

    private TransactionEntity createTransactionEntityWithNullAmount(String transactionId, String storeId) {
        TransactionEntity entity = new TransactionEntity(
                transactionId,
                "CUST-001",
                storeId,
                "TILL-001",
                "card",
                null
        );
        entity.setCurrency("GBP");
        entity.setTransactionTimestamp(ZonedDateTime.now());
        return entity;
    }
}
