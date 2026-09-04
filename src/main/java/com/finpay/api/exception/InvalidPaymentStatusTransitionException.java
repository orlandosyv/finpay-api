package com.finpay.api.exception;

import com.finpay.api.model.PaymentStatus;

public class InvalidPaymentStatusTransitionException
        extends RuntimeException {

    public InvalidPaymentStatusTransitionException(
            PaymentStatus currentStatus,
            PaymentStatus targetStatus) {

        super(
            "Payment cannot transition from "
            + currentStatus
            + " to "
            + targetStatus
        );
    }
}
