package com.finpay.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.finpay.api.repository.PaymentRepository;

@SpringBootTest
@Transactional
class PaymentTimestampIntegrationTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void createsAndUpdatesPaymentTimestamps() {
        Payment payment = new Payment(
                new BigDecimal("25.00"),
                "PEN",
                PaymentStatus.PENDING);

        Payment savedPayment = paymentRepository.saveAndFlush(payment);

        Instant createdAt = savedPayment.getCreatedAt();
        Instant initialUpdatedAt = savedPayment.getUpdatedAt();

        assertThat(createdAt).isNotNull();
        assertThat(initialUpdatedAt).isNotNull();
        assertThat(initialUpdatedAt).isAfterOrEqualTo(createdAt);

        savedPayment.approve();
        paymentRepository.saveAndFlush(savedPayment);

        assertThat(savedPayment.getUpdatedAt()).isAfterOrEqualTo(initialUpdatedAt);
    }
}
