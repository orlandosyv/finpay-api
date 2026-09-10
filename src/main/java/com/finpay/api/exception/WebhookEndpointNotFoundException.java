package com.finpay.api.exception;

public class WebhookEndpointNotFoundException extends RuntimeException {

    public WebhookEndpointNotFoundException(Long id) {
        super("Webhook endpoint with id " + id + " was not found");
    }
}
