package com.vega.techtest.adapter.out.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.vega.techtest.adapter.out.persistence.entity.TransactionEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    @DisplayName("Should correctly aggregate sales by store")
    void testGetTotalSalesByStore() {
        Instant now = Instant.now();
        TransactionEntity t1 = new TransactionEntity(
                "TXN1", "CUST1", "STORE1", "TILL1", "card", new BigDecimal("10.00"), now);
        TransactionEntity t2 = new TransactionEntity(
                "TXN2", "CUST2", "STORE1", "TILL1", "card", new BigDecimal("20.00"), now);
        transactionRepository.saveAll(List.of(t1, t2));

        List<Object[]> results = transactionRepository.getTotalSalesByStore();

        assertThat(results).hasSize(1);
        assertThat(results.get(0)[0]).isEqualTo("STORE1");
        assertThat((BigDecimal) results.get(0)[1]).isEqualByComparingTo("30.00");
    }

    @Test
    @DisplayName("Should return transactions in a date range ordered by timestamp desc")
    void testFindTransactionsByDateRange() {
        Instant baseTime = Instant.parse("2024-01-10T12:00:00Z");
        TransactionEntity older = new TransactionEntity(
                "TXN3", "CUST3", "STORE1", "TILL1", "card", new BigDecimal("5.00"), baseTime);
        older.setTransactionTimestamp(baseTime.minusSeconds(2 * 24 * 60 * 60));
        TransactionEntity newer = new TransactionEntity(
                "TXN4", "CUST4", "STORE1", "TILL1", "card", new BigDecimal("7.00"), baseTime);
        newer.setTransactionTimestamp(baseTime.minusSeconds(24 * 60 * 60));
        transactionRepository.saveAll(List.of(older, newer));

        Instant start = baseTime.minusSeconds(3 * 24 * 60 * 60);
        Instant end = baseTime.minusSeconds(12 * 60 * 60);

        List<TransactionEntity> results = transactionRepository.findTransactionsByDateRange(start, end);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getTransactionId()).isEqualTo("TXN4");
        assertThat(results.get(1).getTransactionId()).isEqualTo("TXN3");
    }

    @Test
    @DisplayName("Should correctly separate sales for different stores")
    void testGetTotalSalesByMultipleStores() {
        // Arrange
        transactionRepository.save(new TransactionEntity("TXN1", "C1", "STORE1", "T1", "card", new BigDecimal("10.00"), Instant.now()));
        transactionRepository.save(new TransactionEntity("TXN2", "C2", "STORE2", "T2", "cash", new BigDecimal("50.00"), Instant.now()));

        // Act
        List<Object[]> results = transactionRepository.getTotalSalesByStore();

        // Assert
        assertThat(results).hasSize(2);
        // Use a map or a more descriptive check to ensure the values match the correct store
        assertThat(results).anySatisfy(row -> {
            assertThat(row[0]).isEqualTo("STORE1");
            assertThat((BigDecimal) row[1]).isEqualByComparingTo("10.00");
        });
        assertThat(results).anySatisfy(row -> {
            assertThat(row[0]).isEqualTo("STORE2");
            assertThat((BigDecimal) row[1]).isEqualByComparingTo("50.00");
        });
    }

    @Test
    @DisplayName("Should be inclusive of start and end boundaries")
    void testDateRangeBoundaries() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(24 * 60 * 60);

        TransactionEntity atStart = new TransactionEntity("T_START", "C1", "S1", "T1", "card", BigDecimal.TEN, start);
        atStart.setTransactionTimestamp(start);

        TransactionEntity atEnd = new TransactionEntity("T_END", "C2", "S1", "T1", "card", BigDecimal.TEN, end);
        atEnd.setTransactionTimestamp(end);

        transactionRepository.saveAll(List.of(atStart, atEnd));

        List<TransactionEntity> results = transactionRepository.findTransactionsByDateRange(start, end);

        // If this fails, you know your JPQL 'BETWEEN' logic needs adjustment
        assertThat(results).hasSize(2);
    }
}
