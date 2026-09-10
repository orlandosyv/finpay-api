package com.finpay.api.dto;

public record WebhookHttpResult(
        boolean successful,
        Integer statusCode,
        String errorMessage) {

    public static WebhookHttpResult succeeded(int statusCode) {
        return new WebhookHttpResult(true, statusCode, null);
    }

    public static WebhookHttpResult failed(Integer statusCode, String message) {
        return new WebhookHttpResult(false, statusCode, message);
    }
}
