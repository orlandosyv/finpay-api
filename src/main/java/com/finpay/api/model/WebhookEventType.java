package com.finpay.api.model;

public enum WebhookEventType {
    PAYMENT_CREATED("payment.created"),
    PAYMENT_APPROVED("payment.approved"),
    PAYMENT_DECLINED("payment.declined"),
    PAYMENT_REFUNDED("payment.refunded");

    private final String eventName;

    WebhookEventType(String eventName) {
        this.eventName = eventName;
    }

    public String getEventName() {
        return eventName;
    }
}
