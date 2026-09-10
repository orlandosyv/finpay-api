package com.finpay.api.exception;

import java.util.UUID;

public class WebhookEventNotFoundException extends RuntimeException {

    public WebhookEventNotFoundException(UUID eventId) {
        super("Webhook event with id " + eventId + " was not found");
    }
}
