package com.vega.techtest.shared.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CalculatorTest {

    @ParameterizedTest(name = "Total {0} / Count {1} should be {2}")
    @CsvSource({
            // Standard Division
            "10.00, 2, 5.00",

            // Transaction Rounding: .5 Rounds to NEAREST EVEN
            "0.1425, 1, 0.14", // 0.1425 -> 0.14 (4 is even)
            "0.1435, 1, 0.14", // 0.1435 -> 0.14 (4 is even)
            "0.1475, 1, 0.15", // 0.1475 -> 0.15 (7 is odd)

            // Complex Division (10 / 7 = 1.428571...)
            "10.00, 7, 1.43",

            // Edge Cases & Resilience
            "0.00, 5, 0.00",   // Zero total
            "10.00, 0, 0.00",  // Guard clause for zero items
            "-10.00, 2, -5.00" // Negative values (important for refunds/adjustments)
    })
    @DisplayName("Should calculate average accurately")
    void calculateAverage_Parameterized(String total, int count, String expected) {
        BigDecimal result = Calculator.calculateAverageAmount(new BigDecimal(total), count);

        // isEqualByComparingTo is essential for BigDecimal because it ignores scale (1.0 vs 1.00)
        assertThat(result).isEqualByComparingTo(expected);
    }

}