package com.finpay.api.exception;

public class ForbiddenRequestException extends RuntimeException {

    public ForbiddenRequestException() {
        super("You do not have permission to access this resource");
    }
}
