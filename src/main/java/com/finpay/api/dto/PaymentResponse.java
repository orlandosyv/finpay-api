package com.finpay.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.finpay.api.model.PaymentStatus;

public record PaymentResponse(
        Long id,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        Instant createdAt,
        Instant updatedAt) {
}
