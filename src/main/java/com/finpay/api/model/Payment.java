package com.finpay.api.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    protected Payment() {
        // Constructor sin parámetros requerido por JPA
    }

    public Payment(
            BigDecimal amount,
            String currency,
            PaymentStatus status) {
        this.amount = amount;
        this.currency = currency;
        this.status = status;
    }

    public Long getId() {
        return id;
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
