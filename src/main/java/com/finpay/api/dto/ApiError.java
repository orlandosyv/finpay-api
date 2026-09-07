package com.finpay.api.dto;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Standard API error response")
public record ApiError(
        @Schema(description = "Time when the error occurred", example = "2026-09-07T20:00:00Z")
        Instant timestamp,
        @Schema(description = "HTTP status code", example = "400")
        int status,
        @Schema(description = "HTTP error name", example = "Bad Request")
        String error,
        @Schema(description = "Human-readable error description", example = "Request validation failed")
        String message,
        @Schema(description = "Requested path", example = "/api/payments")
        String path,
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        @Schema(description = "Validation messages grouped by field")
        Map<String, String> fieldErrors) {

    public ApiError(
            Instant timestamp,
            int status,
            String error,
            String message,
            String path) {
        this(timestamp, status, error, message, path, Map.of());
    }
}
