package com.finpay.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Aggregated webhook event health for the authenticated merchant")
public record WebhookDashboardSummaryResponse(
        long totalEvents,
        long processedEvents,
        long pendingEvents,
        long failedEvents,
        BigDecimal processedRate,
        long totalDeliveries,
        long successfulDeliveries,
        long failedDeliveries,
        BigDecimal deliverySuccessRate,
        Instant from,
        Instant to) {
}
