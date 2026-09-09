package com.finpay.api.exception;

public class UnauthorizedRequestException extends RuntimeException {

    public UnauthorizedRequestException() {
        super("Authentication is required or the access token is invalid");
    }
}
