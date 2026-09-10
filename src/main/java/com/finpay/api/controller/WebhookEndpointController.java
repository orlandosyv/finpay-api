package com.finpay.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.finpay.api.dto.ApiError;
import com.finpay.api.dto.CreateWebhookEndpointRequest;
import com.finpay.api.dto.WebhookEndpointCreatedResponse;
import com.finpay.api.dto.WebhookEndpointResponse;
import com.finpay.api.service.WebhookEndpointService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/merchant/webhooks")
@Tag(name = "Merchant webhooks", description = "Configure signed payment-event notifications")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(
                responseCode = "401",
                description = "Authentication is required or the access token is invalid",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Only MERCHANT_ADMIN can manage webhooks",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
})
public class WebhookEndpointController {

    private final WebhookEndpointService webhookEndpointService;

    public WebhookEndpointController(WebhookEndpointService webhookEndpointService) {
        this.webhookEndpointService = webhookEndpointService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Register a webhook endpoint",
            description = "Returns the signing secret once. The merchant must store it securely.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Webhook endpoint created"),
            @ApiResponse(responseCode = "400", description = "Webhook URL is invalid")
    })
    public WebhookEndpointCreatedResponse create(
            @Valid @RequestBody CreateWebhookEndpointRequest request) {
        return webhookEndpointService.create(request);
    }

    @GetMapping
    @Operation(summary = "List active webhook endpoints")
    @ApiResponse(
            responseCode = "200",
            description = "Active endpoints returned",
            content = @Content(array = @ArraySchema(
                    schema = @Schema(implementation = WebhookEndpointResponse.class))))
    public List<WebhookEndpointResponse> getActiveEndpoints() {
        return webhookEndpointService.getActiveEndpoints();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Disable a webhook endpoint")
    @ApiResponse(responseCode = "404", description = "Webhook endpoint does not exist")
    public void delete(@PathVariable Long id) {
        webhookEndpointService.delete(id);
    }
}
