package com.finpay.api.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "New webhook endpoint and its one-time signing secret")
public record WebhookEndpointCreatedResponse(
        Long id,
        String url,
        boolean active,
        @Schema(description = "Secret used to verify HMAC signatures; returned only once")
        String signingSecret,
        Instant createdAt,
        Instant updatedAt) {
}
