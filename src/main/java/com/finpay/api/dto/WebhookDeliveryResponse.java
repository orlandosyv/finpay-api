package com.finpay.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.finpay.api.model.WebhookDeliveryStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "One HTTP delivery attempt for a webhook event")
public record WebhookDeliveryResponse(
        UUID deliveryId,
        UUID eventId,
        Long endpointId,
        String endpointUrl,
        int attemptNumber,
        WebhookDeliveryStatus status,
        Integer responseStatus,
        String errorMessage,
        Instant attemptedAt) {
}
