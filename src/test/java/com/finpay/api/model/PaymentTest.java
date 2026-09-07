package com.finpay.api.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.finpay.api.exception.InvalidPaymentStatusTransitionException;

class PaymentTest {

    @Test
    void approvesPendingPayment() {
        Payment payment = paymentWithStatus(PaymentStatus.PENDING);

        payment.approve();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
    }

    @Test
    void declinesPendingPayment() {
        Payment payment = paymentWithStatus(PaymentStatus.PENDING);

        payment.decline();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.DECLINED);
    }

    @Test
    void refundsApprovedPayment() {
        Payment payment = paymentWithStatus(PaymentStatus.APPROVED);

        payment.refund();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @ParameterizedTest
    @EnumSource(
            value = PaymentStatus.class,
            names = {"APPROVED", "DECLINED", "REFUNDED"})
    void rejectsApprovalFromNonPendingStatus(PaymentStatus currentStatus) {
        Payment payment = paymentWithStatus(currentStatus);

        assertThatThrownBy(payment::approve)
                .isInstanceOf(InvalidPaymentStatusTransitionException.class)
                .hasMessage("Payment cannot transition from "
                        + currentStatus
                        + " to APPROVED");

        assertThat(payment.getStatus()).isEqualTo(currentStatus);
    }

    @ParameterizedTest
    @EnumSource(
            value = PaymentStatus.class,
            names = {"APPROVED", "DECLINED", "REFUNDED"})
    void rejectsDeclineFromNonPendingStatus(PaymentStatus currentStatus) {
        Payment payment = paymentWithStatus(currentStatus);

        assertThatThrownBy(payment::decline)
                .isInstanceOf(InvalidPaymentStatusTransitionException.class)
                .hasMessage("Payment cannot transition from "
                        + currentStatus
                        + " to DECLINED");

        assertThat(payment.getStatus()).isEqualTo(currentStatus);
    }

    @ParameterizedTest
    @EnumSource(
            value = PaymentStatus.class,
            names = {"PENDING", "DECLINED", "REFUNDED"})
    void rejectsRefundFromNonApprovedStatus(PaymentStatus currentStatus) {
        Payment payment = paymentWithStatus(currentStatus);

        assertThatThrownBy(payment::refund)
                .isInstanceOf(InvalidPaymentStatusTransitionException.class)
                .hasMessage("Payment cannot transition from "
                        + currentStatus
                        + " to REFUNDED");

        assertThat(payment.getStatus()).isEqualTo(currentStatus);
    }

    private Payment paymentWithStatus(PaymentStatus status) {
        return new Payment(
                new BigDecimal("100.00"),
                "PEN",
                status);
    }
}
