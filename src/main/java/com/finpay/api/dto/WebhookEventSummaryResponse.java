package com.finpay.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.finpay.api.model.OutboxEventStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Summary of one payment webhook event")
public record WebhookEventSummaryResponse(
        UUID eventId,
        Long paymentId,
        String eventType,
        OutboxEventStatus status,
        int failedAttempts,
        Instant createdAt,
        Instant nextAttemptAt,
        Instant processedAt) {
}
