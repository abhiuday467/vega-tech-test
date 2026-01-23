package com.vega.techtest.adapter.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Validation error response")
public record ValidationErrorResponse(
        @Schema(example = "error") String status,
        @Schema(example = "Invalid transaction data") String message,
        @Schema(description = "Field-level validation errors") Map<String, String> errors
) {
}
