package com.finpay.api.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.finpay.api.dto.ApiError;
import com.finpay.api.dto.PagedWebhookEventResponse;
import com.finpay.api.dto.WebhookDashboardSummaryResponse;
import com.finpay.api.dto.WebhookDeliveryResponse;
import com.finpay.api.dto.WebhookEventDetailResponse;
import com.finpay.api.model.OutboxEventStatus;
import com.finpay.api.model.WebhookEventType;
import com.finpay.api.service.WebhookDashboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/merchant/webhook-events")
@Tag(name = "Webhook dashboard", description = "Inspect webhook event and delivery health")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(
                responseCode = "401",
                description = "Authentication is required or the access token is invalid",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Only MERCHANT_ADMIN can inspect webhook operations",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
})
public class WebhookDashboardController {

    private final WebhookDashboardService webhookDashboardService;

    public WebhookDashboardController(WebhookDashboardService webhookDashboardService) {
        this.webhookDashboardService = webhookDashboardService;
    }

    @GetMapping
    @Operation(summary = "List webhook events with optional filters")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page returned"),
            @ApiResponse(responseCode = "400", description = "Filter or pagination is invalid")
    })
    public PagedWebhookEventResponse getEvents(
            @RequestParam(required = false) OutboxEventStatus status,
            @RequestParam(required = false) WebhookEventType eventType,
            @RequestParam(required = false) Long paymentId,
            @Parameter(description = "Inclusive ISO-8601 lower bound")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @Parameter(description = "Inclusive ISO-8601 upper bound")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return webhookDashboardService.getEvents(
                status,
                eventType,
                paymentId,
                from,
                to,
                page,
                size);
    }

    @GetMapping("/summary")
    @Operation(summary = "Get aggregated webhook event health")
    public WebhookDashboardSummaryResponse getSummary(
            @Parameter(description = "Inclusive ISO-8601 lower bound")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @Parameter(description = "Inclusive ISO-8601 upper bound")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return webhookDashboardService.getSummary(from, to);
    }

    @GetMapping("/{eventId}")
    @Operation(summary = "Get one webhook event and its immutable payload")
    @ApiResponse(
            responseCode = "404",
            description = "Webhook event does not exist for the authenticated merchant",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public WebhookEventDetailResponse getEvent(@PathVariable UUID eventId) {
        return webhookDashboardService.getEvent(eventId);
    }

    @GetMapping("/{eventId}/deliveries")
    @Operation(summary = "Get the HTTP delivery history for one webhook event")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Delivery attempts returned",
                    content = @Content(array = @ArraySchema(
                            schema = @Schema(implementation = WebhookDeliveryResponse.class)))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Webhook event does not exist for the authenticated merchant",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public List<WebhookDeliveryResponse> getDeliveries(@PathVariable UUID eventId) {
        return webhookDashboardService.getDeliveries(eventId);
    }
}
