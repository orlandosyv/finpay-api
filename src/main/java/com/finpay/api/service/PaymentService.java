package com.finpay.api.service;

import com.finpay.api.model.Payment;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PaymentService {

    public List<Payment> getAllPayments() {
        return List.of(
            new Payment(1L, new BigDecimal("150.00"), "PEN", "APPROVED"),
            new Payment(2L, new BigDecimal("79.90"), "USD", "PENDING")
        );
    }
}
