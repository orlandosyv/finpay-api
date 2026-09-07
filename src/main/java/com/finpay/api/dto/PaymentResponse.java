package com.finpay.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.finpay.api.model.PaymentStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Payment returned to API clients")
public record PaymentResponse(
        @Schema(description = "Payment identifier", example = "1")
        Long id,
        @Schema(description = "Payment amount", example = "125.50")
        BigDecimal amount,
        @Schema(description = "ISO 4217 currency code", example = "PEN")
        String currency,
        @Schema(description = "Current lifecycle status", example = "PENDING")
        PaymentStatus status,
        @Schema(description = "Creation time in UTC", example = "2026-09-07T20:00:00Z")
        Instant createdAt,
        @Schema(description = "Last update time in UTC", example = "2026-09-07T20:05:00Z")
        Instant updatedAt) {
}
