package com.vega.techtest.service;

import com.vega.techtest.dto.TransactionItemResponse;
import com.vega.techtest.dto.TransactionRequest;
import com.vega.techtest.dto.TransactionResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionMetricsServiceTest {

    private MeterRegistry meterRegistry;
    private TransactionMetricsService metricsService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsService = new TransactionMetricsService(meterRegistry);
    }

    @Test
    @DisplayName("Should record all metrics when transaction is submitted")
    void recordTransactionSubmission_recordsAllMetrics() {
        TransactionRequest request = new TransactionRequest();
        request.setStoreId("STORE-001");
        request.setTillId("TILL-001");
        request.setPaymentMethod("card");

        TransactionResponse response = new TransactionResponse();
        response.setTransactionId("TXN-123");
        response.setTotalAmount(new BigDecimal("99.99"));
        response.setItems(createItemList(3));

        metricsService.recordTransactionSubmission(request, response);

        Counter submissionCounter = meterRegistry.find("transaction_submissions_total").counter();
        assertThat(submissionCounter).isNotNull();
        assertThat(submissionCounter.count()).isEqualTo(1.0);

        DistributionSummary amountSummary = meterRegistry.find("transaction_amount").summary();
        assertThat(amountSummary).isNotNull();
        assertThat(amountSummary.count()).isEqualTo(1);
        assertThat(amountSummary.totalAmount()).isEqualTo(99.99);

        DistributionSummary itemCountSummary = meterRegistry.find("transaction_item_count").summary();
        assertThat(itemCountSummary).isNotNull();
        assertThat(itemCountSummary.count()).isEqualTo(1);
        assertThat(itemCountSummary.totalAmount()).isEqualTo(3.0);

        Counter storeCounter = meterRegistry.find("transaction_submissions_by_store")
                .tag("store_id", "STORE-001")
                .counter();
        assertThat(storeCounter).isNotNull();
        assertThat(storeCounter.count()).isEqualTo(1.0);

        Counter tillCounter = meterRegistry.find("transaction_submissions_by_till")
                .tag("till_id", "TILL-001")
                .counter();
        assertThat(tillCounter).isNotNull();
        assertThat(tillCounter.count()).isEqualTo(1.0);

        Counter paymentCounter = meterRegistry.find("transaction_submissions_by_payment_method")
                .tag("payment_method", "card")
                .counter();
        assertThat(paymentCounter).isNotNull();
        assertThat(paymentCounter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should handle null total amount gracefully")
    void recordTransactionSubmission_nullTotalAmount() {
        TransactionRequest request = new TransactionRequest();
        request.setStoreId("STORE-001");
        request.setTillId("TILL-001");
        request.setPaymentMethod("card");

        TransactionResponse response = new TransactionResponse();
        response.setTransactionId("TXN-123");
        response.setTotalAmount(null);
        response.setItems(createItemList(2));

        metricsService.recordTransactionSubmission(request, response);

        Counter submissionCounter = meterRegistry.find("transaction_submissions_total").counter();
        assertThat(submissionCounter).isNotNull();
        assertThat(submissionCounter.count()).isEqualTo(1.0);

        DistributionSummary amountSummary = meterRegistry.find("transaction_amount").summary();
        assertThat(amountSummary).isNotNull();
        assertThat(amountSummary.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle null items gracefully")
    void recordTransactionSubmission_nullItems() {
        TransactionRequest request = new TransactionRequest();
        request.setStoreId("STORE-001");
        request.setTillId("TILL-001");
        request.setPaymentMethod("card");

        TransactionResponse response = new TransactionResponse();
        response.setTransactionId("TXN-123");
        response.setTotalAmount(new BigDecimal("50.00"));
        response.setItems(null);

        metricsService.recordTransactionSubmission(request, response);

        Counter submissionCounter = meterRegistry.find("transaction_submissions_total").counter();
        assertThat(submissionCounter).isNotNull();
        assertThat(submissionCounter.count()).isEqualTo(1.0);

        DistributionSummary itemCountSummary = meterRegistry.find("transaction_item_count").summary();
        assertThat(itemCountSummary).isNotNull();
        assertThat(itemCountSummary.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle null store ID gracefully")
    void recordTransactionSubmission_nullStoreId() {
        TransactionRequest request = new TransactionRequest();
        request.setStoreId(null);
        request.setTillId("TILL-001");
        request.setPaymentMethod("card");

        TransactionResponse response = new TransactionResponse();
        response.setTransactionId("TXN-123");
        response.setTotalAmount(new BigDecimal("50.00"));
        response.setItems(createItemList(1));

        metricsService.recordTransactionSubmission(request, response);

        Counter submissionCounter = meterRegistry.find("transaction_submissions_total").counter();
        assertThat(submissionCounter).isNotNull();
        assertThat(submissionCounter.count()).isEqualTo(1.0);

        Counter storeCounter = meterRegistry.find("transaction_submissions_by_store").counter();
        assertThat(storeCounter).isNull();
    }

    @Test
    @DisplayName("Should handle null till ID gracefully")
    void recordTransactionSubmission_nullTillId() {
        TransactionRequest request = new TransactionRequest();
        request.setStoreId("STORE-001");
        request.setTillId(null);
        request.setPaymentMethod("card");

        TransactionResponse response = new TransactionResponse();
        response.setTransactionId("TXN-123");
        response.setTotalAmount(new BigDecimal("50.00"));
        response.setItems(createItemList(1));

        metricsService.recordTransactionSubmission(request, response);

        Counter submissionCounter = meterRegistry.find("transaction_submissions_total").counter();
        assertThat(submissionCounter).isNotNull();
        assertThat(submissionCounter.count()).isEqualTo(1.0);

        Counter tillCounter = meterRegistry.find("transaction_submissions_by_till").counter();
        assertThat(tillCounter).isNull();
    }

    @Test
    @DisplayName("Should handle null payment method gracefully")
    void recordTransactionSubmission_nullPaymentMethod() {
        TransactionRequest request = new TransactionRequest();
        request.setStoreId("STORE-001");
        request.setTillId("TILL-001");
        request.setPaymentMethod(null);

        TransactionResponse response = new TransactionResponse();
        response.setTransactionId("TXN-123");
        response.setTotalAmount(new BigDecimal("50.00"));
        response.setItems(createItemList(1));

        metricsService.recordTransactionSubmission(request, response);

        Counter submissionCounter = meterRegistry.find("transaction_submissions_total").counter();
        assertThat(submissionCounter).isNotNull();
        assertThat(submissionCounter.count()).isEqualTo(1.0);

        Counter paymentCounter = meterRegistry.find("transaction_submissions_by_payment_method").counter();
        assertThat(paymentCounter).isNull();
    }

    @Test
    @DisplayName("Should record transaction retrieval metric")
    void recordTransactionRetrieval_incrementsCounter() {
        metricsService.recordTransactionRetrieval();

        Counter retrievalCounter = meterRegistry.find("transaction_retrievals_total").counter();
        assertThat(retrievalCounter).isNotNull();
        assertThat(retrievalCounter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should handle empty items list")
    void recordTransactionSubmission_emptyItems() {
        TransactionRequest request = new TransactionRequest();
        request.setStoreId("STORE-001");
        request.setTillId("TILL-001");
        request.setPaymentMethod("cash");

        TransactionResponse response = new TransactionResponse();
        response.setTransactionId("TXN-123");
        response.setTotalAmount(new BigDecimal("25.00"));
        response.setItems(new ArrayList<>());

        metricsService.recordTransactionSubmission(request, response);

        Counter submissionCounter = meterRegistry.find("transaction_submissions_total").counter();
        assertThat(submissionCounter).isNotNull();
        assertThat(submissionCounter.count()).isEqualTo(1.0);

        DistributionSummary itemCountSummary = meterRegistry.find("transaction_item_count").summary();
        assertThat(itemCountSummary).isNotNull();
        assertThat(itemCountSummary.totalAmount()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should record multiple submissions correctly")
    void recordTransactionSubmission_multipleSubmissions() {
        TransactionRequest request1 = new TransactionRequest();
        request1.setStoreId("STORE-001");
        request1.setTillId("TILL-001");
        request1.setPaymentMethod("card");

        TransactionResponse response1 = new TransactionResponse();
        response1.setTransactionId("TXN-123");
        response1.setTotalAmount(new BigDecimal("50.00"));
        response1.setItems(createItemList(2));

        TransactionRequest request2 = new TransactionRequest();
        request2.setStoreId("STORE-002");
        request2.setTillId("TILL-002");
        request2.setPaymentMethod("cash");

        TransactionResponse response2 = new TransactionResponse();
        response2.setTransactionId("TXN-124");
        response2.setTotalAmount(new BigDecimal("75.00"));
        response2.setItems(createItemList(3));

        metricsService.recordTransactionSubmission(request1, response1);
        metricsService.recordTransactionSubmission(request2, response2);

        Counter submissionCounter = meterRegistry.find("transaction_submissions_total").counter();
        assertThat(submissionCounter).isNotNull();
        assertThat(submissionCounter.count()).isEqualTo(2.0);

        DistributionSummary amountSummary = meterRegistry.find("transaction_amount").summary();
        assertThat(amountSummary).isNotNull();
        assertThat(amountSummary.count()).isEqualTo(2);
        assertThat(amountSummary.totalAmount()).isEqualTo(125.00);
    }

    private List<TransactionItemResponse> createItemList(int count) {
        List<TransactionItemResponse> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            TransactionItemResponse item = new TransactionItemResponse();
            item.setProductName("Product-" + i);
            item.setUnitPrice(new BigDecimal("10.00"));
            item.setQuantity(1);
            items.add(item);
        }
        return items;
    }
}
