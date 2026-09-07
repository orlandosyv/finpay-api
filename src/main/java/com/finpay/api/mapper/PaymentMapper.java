package com.finpay.api.mapper;

import org.springframework.stereotype.Component;

import com.finpay.api.dto.PaymentResponse;
import com.finpay.api.model.Payment;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus());
    }
}
