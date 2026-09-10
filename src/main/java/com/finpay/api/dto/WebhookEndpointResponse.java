package com.finpay.api.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Active webhook endpoint owned by the authenticated merchant")
public record WebhookEndpointResponse(
        Long id,
        String url,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {
}
