package com.finpay.api.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finpay.api.dto.WebhookHttpResult;
import com.finpay.api.model.OutboxEvent;
import com.finpay.api.model.WebhookDelivery;
import com.finpay.api.model.WebhookDeliveryStatus;
import com.finpay.api.model.WebhookEndpoint;
import com.finpay.api.repository.OutboxEventRepository;
import com.finpay.api.repository.WebhookDeliveryRepository;
import com.finpay.api.repository.WebhookEndpointRepository;

@Service
public class WebhookOutboxStateService {

    private final OutboxEventRepository outboxEventRepository;
    private final WebhookEndpointRepository webhookEndpointRepository;
    private final WebhookDeliveryRepository webhookDeliveryRepository;
    private final WebhookRetryPolicy retryPolicy;

    public WebhookOutboxStateService(
            OutboxEventRepository outboxEventRepository,
            WebhookEndpointRepository webhookEndpointRepository,
            WebhookDeliveryRepository webhookDeliveryRepository,
            WebhookRetryPolicy retryPolicy) {
        this.outboxEventRepository = outboxEventRepository;
        this.webhookEndpointRepository = webhookEndpointRepository;
        this.webhookDeliveryRepository = webhookDeliveryRepository;
        this.retryPolicy = retryPolicy;
    }

    @Transactional
    public void recordDelivery(
            UUID eventId,
            Long endpointId,
            int attemptNumber,
            WebhookHttpResult result) {
        OutboxEvent event = outboxEventRepository.getReferenceById(eventId);
        WebhookEndpoint endpoint = webhookEndpointRepository.getReferenceById(endpointId);
        webhookDeliveryRepository.save(new WebhookDelivery(
                event,
                endpoint,
                attemptNumber,
                result.successful()
                        ? WebhookDeliveryStatus.SUCCEEDED
                        : WebhookDeliveryStatus.FAILED,
                result.statusCode(),
                truncate(result.errorMessage()),
                Instant.now()));
    }

    @Transactional
    public void markProcessed(UUID eventId) {
        OutboxEvent event = outboxEventRepository.findById(eventId).orElseThrow();
        event.markProcessed(Instant.now());
    }

    @Transactional
    public void retryOrFail(UUID eventId) {
        OutboxEvent event = outboxEventRepository.findById(eventId).orElseThrow();
        int completedAttempts = event.getAttempts() + 1;
        if (retryPolicy.hasAnotherAttempt(completedAttempts)) {
            event.scheduleRetry(Instant.now().plus(
                    retryPolicy.delayAfterFailure(completedAttempts)));
        } else {
            event.markFailed();
        }
    }

    private String truncate(String message) {
        if (message == null || message.length() <= 1000) {
            return message;
        }
        return message.substring(0, 1000);
    }
}
