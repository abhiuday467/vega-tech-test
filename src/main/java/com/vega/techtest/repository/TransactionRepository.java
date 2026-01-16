package com.vega.techtest.repository;

import com.vega.techtest.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    Optional<TransactionEntity> findByTransactionId(String transactionId);

    List<TransactionEntity> findByStoreIdOrderByTransactionTimestampDesc(String storeId);

    List<TransactionEntity> findByCustomerIdOrderByTransactionTimestampDesc(String customerId);

    List<TransactionEntity> findByTillIdOrderByTransactionTimestampDesc(String tillId);

    @Query("SELECT t FROM TransactionEntity t WHERE t.transactionTimestamp BETWEEN :startDate AND :endDate ORDER BY t.transactionTimestamp DESC")
    List<TransactionEntity> findTransactionsByDateRange(@Param("startDate") ZonedDateTime startDate,
                                                        @Param("endDate") ZonedDateTime endDate);

    List<TransactionEntity> findByPaymentMethodOrderByTransactionTimestampDesc(String paymentMethod);

    @Query("SELECT t.storeId, COUNT(t) FROM TransactionEntity t GROUP BY t.storeId")
    List<Object[]> countTransactionsByStore();

    @Query("SELECT t.storeId, SUM(t.totalAmount) FROM TransactionEntity t GROUP BY t.storeId")
    List<Object[]> getTotalSalesByStore();

    boolean existsByTransactionId(String transactionId);
}
