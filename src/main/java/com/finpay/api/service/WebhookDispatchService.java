package com.finpay.api.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.finpay.api.dto.WebhookHttpResult;
import com.finpay.api.model.OutboxEvent;
import com.finpay.api.model.OutboxEventStatus;
import com.finpay.api.model.WebhookDeliveryStatus;
import com.finpay.api.repository.OutboxEventRepository;
import com.finpay.api.repository.WebhookDeliveryRepository;
import com.finpay.api.repository.WebhookEndpointRepository;

@Service
public class WebhookDispatchService {

    private final OutboxEventRepository outboxEventRepository;
    private final WebhookEndpointRepository webhookEndpointRepository;
    private final WebhookDeliveryRepository webhookDeliveryRepository;
    private final WebhookSecretCrypto secretCrypto;
    private final WebhookSignatureService signatureService;
    private final WebhookHttpClient httpClient;
    private final WebhookOutboxStateService stateService;

    public WebhookDispatchService(
            OutboxEventRepository outboxEventRepository,
            WebhookEndpointRepository webhookEndpointRepository,
            WebhookDeliveryRepository webhookDeliveryRepository,
            WebhookSecretCrypto secretCrypto,
            WebhookSignatureService signatureService,
            WebhookHttpClient httpClient,
            WebhookOutboxStateService stateService) {
        this.outboxEventRepository = outboxEventRepository;
        this.webhookEndpointRepository = webhookEndpointRepository;
        this.webhookDeliveryRepository = webhookDeliveryRepository;
        this.secretCrypto = secretCrypto;
        this.signatureService = signatureService;
        this.httpClient = httpClient;
        this.stateService = stateService;
    }

    public void dispatchPendingEvents() {
        outboxEventRepository
                .findTop20ByStatusAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
                        OutboxEventStatus.PENDING,
                        Instant.now())
                .forEach(this::dispatch);
    }

    private void dispatch(OutboxEvent event) {
        var endpoints = webhookEndpointRepository
                .findAllByMerchantIdAndActiveTrueOrderByIdAsc(event.getMerchantId());
        if (endpoints.isEmpty()) {
            stateService.markProcessed(event.getId());
            return;
        }

        boolean allSucceeded = true;
        int attemptNumber = event.getAttempts() + 1;
        for (var endpoint : endpoints) {
            if (webhookDeliveryRepository.existsByEventIdAndWebhookEndpointIdAndStatus(
                    event.getId(),
                    endpoint.getId(),
                    WebhookDeliveryStatus.SUCCEEDED)) {
                continue;
            }

            String timestamp = Long.toString(Instant.now().getEpochSecond());
            WebhookHttpResult result;
            try {
                String signingSecret = secretCrypto.decrypt(endpoint.getSecretEncrypted());
                String signature = signatureService.sign(
                        signingSecret,
                        timestamp,
                        event.getPayload());
                result = httpClient.post(endpoint, event, timestamp, signature);
            } catch (RuntimeException exception) {
                result = WebhookHttpResult.failed(null, exception.getMessage());
            }

            stateService.recordDelivery(
                    event.getId(),
                    endpoint.getId(),
                    attemptNumber,
                    result);
            allSucceeded &= result.successful();
        }

        if (allSucceeded) {
            stateService.markProcessed(event.getId());
        } else {
            stateService.retryOrFail(event.getId());
        }
    }
}
