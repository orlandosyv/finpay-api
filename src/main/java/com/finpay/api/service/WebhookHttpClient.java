package com.finpay.api.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.stereotype.Service;

import com.finpay.api.dto.WebhookHttpResult;
import com.finpay.api.model.OutboxEvent;
import com.finpay.api.model.WebhookEndpoint;

@Service
public class WebhookHttpClient {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private final WebhookUrlValidator urlValidator;

    public WebhookHttpClient(WebhookUrlValidator urlValidator) {
        this.urlValidator = urlValidator;
    }

    public WebhookHttpResult post(
            WebhookEndpoint endpoint,
            OutboxEvent event,
            String timestamp,
            String signature) {
        try {
            String safeUrl = urlValidator.validateAndNormalize(endpoint.getUrl());
            HttpRequest request = HttpRequest.newBuilder(URI.create(safeUrl))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "FinPay-Webhooks/1.0")
                    .header("X-FinPay-Event-Id", event.getId().toString())
                    .header("X-FinPay-Event-Type", event.getEventType().getEventName())
                    .header("X-FinPay-Timestamp", timestamp)
                    .header("X-FinPay-Signature", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(event.getPayload()))
                    .build();

            HttpResponse<Void> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return WebhookHttpResult.succeeded(response.statusCode());
            }
            return WebhookHttpResult.failed(
                    response.statusCode(),
                    "Webhook returned HTTP " + response.statusCode());
        } catch (Exception exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            String message = exception.getMessage() == null
                    ? exception.getClass().getSimpleName()
                    : exception.getMessage();
            return WebhookHttpResult.failed(null, message);
        }
    }
}
