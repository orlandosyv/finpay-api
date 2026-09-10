package com.finpay.api.dto;

public record IdempotentPaymentResult(
        PaymentResponse payment,
        boolean replayed) {
}
