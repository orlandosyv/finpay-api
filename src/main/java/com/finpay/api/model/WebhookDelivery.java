package com.finpay.api.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "webhook_deliveries",
        uniqueConstraints = @UniqueConstraint(
                name = "UQ_webhook_delivery_attempt",
                columnNames = {"event_id", "webhook_endpoint_id", "attempt_number"}))
public class WebhookDelivery {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private OutboxEvent event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "webhook_endpoint_id", nullable = false)
    private WebhookEndpoint webhookEndpoint;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WebhookDeliveryStatus status;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    protected WebhookDelivery() {
        // No-argument constructor required by JPA.
    }

    public WebhookDelivery(
            OutboxEvent event,
            WebhookEndpoint webhookEndpoint,
            int attemptNumber,
            WebhookDeliveryStatus status,
            Integer responseStatus,
            String errorMessage,
            Instant attemptedAt) {
        this.id = UUID.randomUUID();
        this.event = event;
        this.webhookEndpoint = webhookEndpoint;
        this.attemptNumber = attemptNumber;
        this.status = status;
        this.responseStatus = responseStatus;
        this.errorMessage = errorMessage;
        this.attemptedAt = attemptedAt;
    }
}
