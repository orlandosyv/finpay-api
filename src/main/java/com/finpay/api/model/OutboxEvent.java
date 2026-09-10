package com.finpay.api.model;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private WebhookEventType eventType;

    @Column(nullable = false, length = 4000)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxEventStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "available_at", nullable = false)
    private Instant availableAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    protected OutboxEvent() {
        // No-argument constructor required by JPA.
    }

    public OutboxEvent(
            UUID id,
            Merchant merchant,
            Long aggregateId,
            WebhookEventType eventType,
            String payload,
            Instant availableAt) {
        this.id = id;
        this.merchant = merchant;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = OutboxEventStatus.PENDING;
        this.attempts = 0;
        this.availableAt = availableAt;
    }

    public UUID getId() {
        return id;
    }

    public Long getMerchantId() {
        return merchant.getId();
    }

    public Long getAggregateId() {
        return aggregateId;
    }

    public WebhookEventType getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public OutboxEventStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public Instant getAvailableAt() {
        return availableAt;
    }

    public void markProcessed(Instant processedAt) {
        status = OutboxEventStatus.PROCESSED;
        this.processedAt = processedAt;
    }

    public void scheduleRetry(Instant nextAttemptAt) {
        attempts++;
        status = OutboxEventStatus.PENDING;
        availableAt = nextAttemptAt;
    }

    public void markFailed() {
        attempts++;
        status = OutboxEventStatus.FAILED;
    }
}
