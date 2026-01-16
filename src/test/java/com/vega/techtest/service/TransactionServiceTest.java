package com.vega.techtest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vega.techtest.dto.TransactionRequest;
import com.vega.techtest.dto.TransactionResponse;
import com.vega.techtest.entity.TransactionEntity;
import com.vega.techtest.repository.TransactionRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
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

        when(transactionRepository.existsByTransactionId(any())).thenReturn(false);
        when(transactionRepository.save(any(TransactionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = transactionService.processTransaction(request);

        assertThat(response.getTransactionId()).startsWith("TXN-");
        assertThat(response.getStoreId()).isEqualTo("STORE-1");
        assertThat(response.getPaymentMethod()).isEqualTo("card");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("12.50");
        assertThat(response.getCurrency()).isEqualTo("GBP");

        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        verify(transactionRepository).existsByTransactionId(idCaptor.capture());
        assertThat(idCaptor.getValue()).isEqualTo(response.getTransactionId());
    }

    @Test
    @DisplayName("Should reject duplicate transaction id")
    void processTransaction_rejectsDuplicateId() {
        TransactionRequest request = new TransactionRequest();
        request.setTransactionId("TXN-EXIST");
        request.setStoreId("STORE-1");
        request.setTillId("TILL-1");
        request.setPaymentMethod("cash");
        request.setTotalAmount(new BigDecimal("9.99"));

        when(transactionRepository.existsByTransactionId("TXN-EXIST")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> transactionService.processTransaction(request));
    }
}
