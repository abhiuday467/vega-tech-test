package com.vega.techtest.adapter.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Generic error response")
public record ErrorResponse(
        @Schema(example = "error") String status,
        @Schema(example = "Failed to process request") String message,
        @Schema(example = "Internal server error") String error
) {
}
