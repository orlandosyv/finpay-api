package com.finpay.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "finpay.webhook.scheduler-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class WebhookDispatchScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookDispatchScheduler.class);

    private final WebhookDispatchService dispatchService;

    public WebhookDispatchScheduler(WebhookDispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @Scheduled(
            initialDelayString = "${finpay.webhook.scheduler-initial-delay-ms:5000}",
            fixedDelayString = "${finpay.webhook.scheduler-delay-ms:5000}")
    public void dispatch() {
        try {
            dispatchService.dispatchPendingEvents();
        } catch (RuntimeException exception) {
            LOGGER.error("Webhook dispatch cycle failed", exception);
        }
    }
}
