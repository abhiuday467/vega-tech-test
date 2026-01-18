package com.vega.techtest.domain.transaction.exception;

import java.math.BigDecimal;

public class ReceiptTotalMismatchException extends RuntimeException {

    private final BigDecimal calculatedTotal;
    private final BigDecimal providedTotal;

    public ReceiptTotalMismatchException(String message, BigDecimal calculatedTotal, BigDecimal providedTotal) {
        super(message);
        this.calculatedTotal = calculatedTotal;
        this.providedTotal = providedTotal;
    }

    public BigDecimal getCalculatedTotal() {
        return calculatedTotal;
    }

    public BigDecimal getProvidedTotal() {
        return providedTotal;
    }
}
