package com.finpay.api.model;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import com.finpay.api.exception.InvalidPaymentStatusTransitionException;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Payment() {
        // Constructor sin parámetros requerido por JPA
    }

    public Payment(
            Merchant merchant,
            BigDecimal amount,
            String currency,
            PaymentStatus status) {
        this.merchant = merchant;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public Merchant getMerchant() {
        return merchant;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void approve() {
        if (status != PaymentStatus.PENDING) {
            throw new InvalidPaymentStatusTransitionException(
                    status,
                    PaymentStatus.APPROVED);
        }

        status = PaymentStatus.APPROVED;
    }

    public void decline() {
        if (status != PaymentStatus.PENDING) {
            throw new InvalidPaymentStatusTransitionException(
                status,
                PaymentStatus.DECLINED
            );
        }

        status = PaymentStatus.DECLINED;
    }

    public void refund() {
        if (status != PaymentStatus.APPROVED) {
            throw new InvalidPaymentStatusTransitionException(
                status,
                PaymentStatus.REFUNDED
            );
        }

        status = PaymentStatus.REFUNDED;
    }

}
