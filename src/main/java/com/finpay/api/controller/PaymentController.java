package com.finpay.api.controller;

import com.finpay.api.dto.CreatePaymentRequest;
import com.finpay.api.dto.PaymentResponse;
import com.finpay.api.mapper.PaymentMapper;
import com.finpay.api.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;

    public PaymentController(
            PaymentService paymentService,
            PaymentMapper paymentMapper) {
        this.paymentService = paymentService;
        this.paymentMapper = paymentMapper;
    }

    @GetMapping
    public List<PaymentResponse> getPayments() {
        return paymentService.getAllPayments()
                .stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public PaymentResponse getPaymentById(@PathVariable Long id) {
        return paymentMapper.toResponse(paymentService.getPaymentById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        return paymentMapper.toResponse(paymentService.createPayment(request));
    }

    @PatchMapping("/{id}/approve")
    public PaymentResponse approvePayment(@PathVariable Long id) {
        return paymentMapper.toResponse(paymentService.approvePayment(id));
    }

    @PatchMapping("/{id}/decline")
    public PaymentResponse declinePayment(@PathVariable Long id) {
        return paymentMapper.toResponse(paymentService.declinePayment(id));
    }

    @PatchMapping("/{id}/refund")
    public PaymentResponse refundPayment(@PathVariable Long id) {
        return paymentMapper.toResponse(paymentService.refundPayment(id));
    }

}
