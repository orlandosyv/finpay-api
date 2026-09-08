package com.finpay.api.exception;

public class MerchantNotFoundException extends RuntimeException {

    public MerchantNotFoundException(Long id) {
        super("Merchant with id " + id + " was not found");
    }
}
