package com.vega.techtest.shared.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static java.math.BigDecimal.valueOf;

//TODO Add warning logging for division by zero and for zero sum transaction to raise suspicion.
//This needs to go all the way to Metrics as I want to put one item for suspicious warning
public class Calculator {
    private static final int INTERNAL_CALC_SCALE = 6;
    private static final int FINAL_DISPLAY_SCALE = 2;

    public static BigDecimal calculateAverageAmount(BigDecimal totalAmount, int count) {
        if (count <= 0) {
            return BigDecimal.ZERO.setScale(FINAL_DISPLAY_SCALE, RoundingMode.HALF_EVEN);
        }
        BigDecimal safeTotal = (totalAmount == null) ? BigDecimal.ZERO : totalAmount;
        BigDecimal intermediateResult = safeTotal.divide(
                valueOf(count),
                INTERNAL_CALC_SCALE,
                RoundingMode.HALF_EVEN
        );

        return intermediateResult.setScale(FINAL_DISPLAY_SCALE, RoundingMode.HALF_EVEN);
    }
}
