package com.vega.techtest.service;

import com.vega.techtest.dto.TransactionRequest;
import com.vega.techtest.dto.TransactionResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TransactionMetricsService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionMetricsService.class);

    private final MeterRegistry meterRegistry;
    private final Counter transactionSubmissionCounter;
    private final Counter transactionRetrievalCounter;
    private final DistributionSummary transactionAmountSummary;
    private final DistributionSummary transactionItemCountSummary;

    public TransactionMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.transactionSubmissionCounter = Counter.builder("transaction_submissions_total")
                .description("Total number of transaction submissions via REST API")
                .register(meterRegistry);

        this.transactionRetrievalCounter = Counter.builder("transaction_retrievals_total")
                .description("Total number of transaction retrievals via REST API")
                .register(meterRegistry);

        this.transactionAmountSummary = DistributionSummary.builder("transaction_amount")
                .description("Distribution of transaction amounts")
                .baseUnit("GBP")
                .register(meterRegistry);

        this.transactionItemCountSummary = DistributionSummary.builder("transaction_item_count")
                .description("Distribution of number of items per transaction")
                .baseUnit("items")
                .register(meterRegistry);
    }

    public void recordTransactionSubmission(TransactionRequest request, TransactionResponse response) {
        try {
            transactionSubmissionCounter.increment();

            if (response.totalAmount() != null) {
                transactionAmountSummary.record(response.totalAmount().doubleValue());
            }

            if (response.items() != null) {
                transactionItemCountSummary.record(response.items().size());
            }

            if (request.storeId() != null) {
                Counter.builder("transaction_submissions_by_store")
                        .tag("store_id", request.storeId())
                        .description("Transaction submissions by store")
                        .register(meterRegistry)
                        .increment();
            }

            if (request.tillId() != null) {
                Counter.builder("transaction_submissions_by_till")
                        .tag("till_id", request.tillId())
                        .description("Transaction submissions by till")
                        .register(meterRegistry)
                        .increment();
            }

            if (request.paymentMethod() != null) {
                Counter.builder("transaction_submissions_by_payment_method")
                        .tag("payment_method", request.paymentMethod())
                        .description("Transaction submissions by payment method")
                        .register(meterRegistry)
                        .increment();
            }

            logger.debug("Recorded metrics for transaction: {}", response.transactionId());
        } catch (Exception e) {
            logger.warn("Failed to record transaction submission metrics: {}", e.getMessage());
        }
    }

    public void recordTransactionRetrieval() {
        try {
            transactionRetrievalCounter.increment();
        } catch (Exception e) {
            logger.warn("Failed to record transaction retrieval metrics: {}", e.getMessage());
        }
    }
}
