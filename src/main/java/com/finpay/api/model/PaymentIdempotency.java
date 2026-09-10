package com.finpay.api.model;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import com.finpay.api.dto.PaymentResponse;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "payment_idempotency_keys",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UQ_payment_idempotency_merchant_key",
                        columnNames = {"merchant_id", "idempotency_key"}),
                @UniqueConstraint(
                        name = "UQ_payment_idempotency_payment",
                        columnNames = "payment_id")
        })
public class PaymentIdempotency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "original_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal originalAmount;

    @Column(name = "original_currency", nullable = false, length = 3)
    private String originalCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "original_status", nullable = false, length = 20)
    private PaymentStatus originalStatus;

    @Column(name = "original_created_at", nullable = false)
    private Instant originalCreatedAt;

    @Column(name = "original_updated_at", nullable = false)
    private Instant originalUpdatedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PaymentIdempotency() {
        // No-argument constructor required by JPA.
    }

    public PaymentIdempotency(
            Merchant merchant,
            String idempotencyKey,
            String requestHash,
            Payment payment,
            PaymentResponse originalResponse) {
        this.merchant = merchant;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.payment = payment;
        this.originalAmount = originalResponse.amount();
        this.originalCurrency = originalResponse.currency();
        this.originalStatus = originalResponse.status();
        this.originalCreatedAt = originalResponse.createdAt();
        this.originalUpdatedAt = originalResponse.updatedAt();
    }

    public String getRequestHash() {
        return requestHash;
    }

    public PaymentResponse toOriginalResponse() {
        return new PaymentResponse(
                payment.getId(),
                originalAmount,
                originalCurrency,
                originalStatus,
                originalCreatedAt,
                originalUpdatedAt);
    }
}
