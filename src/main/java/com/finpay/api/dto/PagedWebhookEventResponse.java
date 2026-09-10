package com.finpay.api.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "One page of webhook events")
public record PagedWebhookEventResponse(
        List<WebhookEventSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
