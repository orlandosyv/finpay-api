package com.finpay.api.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finpay.api.dto.CreatePaymentRequest;
import com.finpay.api.model.Payment;
import com.finpay.api.repository.PaymentRepository;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    @Transactional
    public Payment createPayment(CreatePaymentRequest request) {

        Payment payment = new Payment(
            request.getAmount(),
            request.getCurrency(),
            "PENDING"
        );

        return paymentRepository.save(payment);
    }
}
