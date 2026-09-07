package com.finpay.api.controller;

import com.finpay.api.dto.CreatePaymentRequest;
import com.finpay.api.model.Payment;
import com.finpay.api.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public List<Payment> getPayments() {
        return paymentService.getAllPayments();
    }

    @GetMapping("/{id}")
    public Payment getPaymentById(@PathVariable Long id) {
        return paymentService.getPaymentById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Payment createPayment(@RequestBody CreatePaymentRequest request) {
        return paymentService.createPayment(request);
    }

    @PatchMapping("/{id}/approve")
    public Payment approvePayment(@PathVariable Long id) {
        return paymentService.approvePayment(id);
    }

    @PatchMapping("/{id}/decline")
    public Payment declinePayment(@PathVariable Long id) {
        return paymentService.declinePayment(id);
    }

    @PatchMapping("/{id}/refund")
    public Payment refundPayment(@PathVariable Long id) {
        return paymentService.refundPayment(id);
    }

}
