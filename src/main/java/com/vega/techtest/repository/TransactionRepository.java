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

    @Query("SELECT t.storeId, SUM(t.totalAmount) FROM TransactionEntity t GROUP BY t.storeId")
    List<Object[]> getTotalSalesByStore();

    //TODO Remove any index for findByPaymentMethodOrderByTransactionTimestampDesc and the composite indexing to improve the performance
    //TODO Remove any index for countTransactionsByStore
    //TODO Remove any index for getTotalSalesByStore
    //TODO Check we are UTC time in the database
    //TODO Check the Transaction pattern is in TXN-UUID
    //TODO add swagger

    TransactionEntity findByStoreIdAndTillIdAndTransactionTimestamp(String storeId, String tillId,ZonedDateTime timestamp);
}
