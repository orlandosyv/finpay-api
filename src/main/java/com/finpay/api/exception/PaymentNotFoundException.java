package com.finpay.api.exception;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(Long id) {
        super("Payment with id " + id + " was not found");
    }
}
