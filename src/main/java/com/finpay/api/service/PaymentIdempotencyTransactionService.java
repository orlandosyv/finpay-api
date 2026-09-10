package com.finpay.api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finpay.api.dto.CreatePaymentRequest;
import com.finpay.api.dto.IdempotentPaymentResult;
import com.finpay.api.dto.PaymentResponse;
import com.finpay.api.mapper.PaymentMapper;
import com.finpay.api.model.Payment;
import com.finpay.api.model.PaymentIdempotency;
import com.finpay.api.repository.PaymentIdempotencyRepository;
import com.finpay.api.repository.PaymentRepository;

@Service
public class PaymentIdempotencyTransactionService {

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final PaymentIdempotencyRepository idempotencyRepository;
    private final PaymentMapper paymentMapper;

    public PaymentIdempotencyTransactionService(
            PaymentService paymentService,
            PaymentRepository paymentRepository,
            PaymentIdempotencyRepository idempotencyRepository,
            PaymentMapper paymentMapper) {
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.paymentMapper = paymentMapper;
    }

    @Transactional
    public IdempotentPaymentResult create(
            String idempotencyKey,
            String requestHash,
            CreatePaymentRequest request) {
        Payment payment = paymentService.createPayment(request);

        // Force SQL execution so timestamps are available and both inserts fail or commit together.
        paymentRepository.flush();
        PaymentResponse originalResponse = paymentMapper.toResponse(payment);

        idempotencyRepository.saveAndFlush(new PaymentIdempotency(
                payment.getMerchant(),
                idempotencyKey,
                requestHash,
                payment,
                originalResponse));

        return new IdempotentPaymentResult(originalResponse, false);
    }
}
