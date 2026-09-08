package com.finpay.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.finpay.api.dto.PaymentResponse;
import com.finpay.api.model.Merchant;
import com.finpay.api.model.MerchantStatus;
import com.finpay.api.model.Payment;
import com.finpay.api.model.PaymentStatus;

class PaymentMapperTest {

    private final PaymentMapper paymentMapper = new PaymentMapper();

    @Test
    void mapsPaymentToPublicResponse() {
        Payment payment = new Payment(
                new Merchant("Test Merchant", MerchantStatus.ACTIVE),
                new BigDecimal("250.00"),
                "PEN",
                PaymentStatus.PENDING);

        PaymentResponse response = paymentMapper.toResponse(payment);

        assertThat(response.id()).isNull();
        assertThat(response.amount()).isEqualByComparingTo("250.00");
        assertThat(response.currency()).isEqualTo("PEN");
        assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.createdAt()).isNull();
        assertThat(response.updatedAt()).isNull();
    }
}
