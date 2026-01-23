package com.vega.techtest.adapter.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Receipt total mismatch response")
public record ReceiptTotalMismatchResponse(
        @Schema(example = "error") String status,
        @Schema(example = "Receipt total mismatch") String message,
        @Schema(example = "Totals do not match") String error,
        @Schema(example = "7.69") BigDecimal calculatedTotal,
        @Schema(example = "7.70") BigDecimal providedTotal
) {
}
