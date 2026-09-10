package com.finpay.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.finpay.api.model.OutboxEventStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

@Schema(description = "Detailed webhook event including the immutable delivery payload")
public record WebhookEventDetailResponse(
        UUID eventId,
        Long paymentId,
        String eventType,
        OutboxEventStatus status,
        int failedAttempts,
        Instant createdAt,
        Instant nextAttemptAt,
        Instant processedAt,
        JsonNode payload) {
}
