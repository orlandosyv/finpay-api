package com.finpay.api.dto;

import java.math.BigDecimal;

import com.finpay.api.model.PaymentStatus;

public record PaymentResponse(
        Long id,
        BigDecimal amount,
        String currency,
        PaymentStatus status) {
}
