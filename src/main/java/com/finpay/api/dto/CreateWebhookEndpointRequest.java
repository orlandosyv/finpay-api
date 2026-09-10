package com.finpay.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Webhook destination registered by a merchant administrator")
public class CreateWebhookEndpointRequest {

    @Schema(
            description = "HTTP or HTTPS URL that receives payment events",
            example = "https://merchant.example.com/webhooks/finpay")
    @NotBlank(message = "Webhook URL is required")
    @Size(max = 2048, message = "Webhook URL must not exceed 2048 characters")
    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
