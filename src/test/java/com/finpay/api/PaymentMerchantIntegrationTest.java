package com.finpay.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.finpay.api.config.TestcontainersConfiguration;
import com.finpay.api.dto.CreatePaymentRequest;
import com.finpay.api.exception.PaymentNotFoundException;
import com.finpay.api.model.Merchant;
import com.finpay.api.model.MerchantStatus;
import com.finpay.api.model.Payment;
import com.finpay.api.model.PaymentStatus;
import com.finpay.api.repository.MerchantRepository;
import com.finpay.api.repository.PaymentRepository;
import com.finpay.api.service.PaymentService;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class PaymentMerchantIntegrationTest {

    private static final Long DEFAULT_MERCHANT_ID = 1L;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
    }

    @Test
    void assignsNewPaymentToDefaultMerchant() {
        Payment payment = paymentService.createPayment(paymentRequest("125.50", "PEN"));

        Payment storedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(storedPayment.getMerchant().getId()).isEqualTo(DEFAULT_MERCHANT_ID);
        assertThat(storedPayment.getMerchant().getName()).isEqualTo("Demo Merchant");
    }

    @Test
    void listsOnlyPaymentsOwnedByCurrentMerchant() {
        Merchant defaultMerchant = merchantRepository.findById(DEFAULT_MERCHANT_ID).orElseThrow();
        Merchant otherMerchant = merchantRepository.saveAndFlush(
                new Merchant("Other Merchant", MerchantStatus.ACTIVE));

        Payment ownedPayment = paymentRepository.saveAndFlush(new Payment(
                defaultMerchant,
                new BigDecimal("50.00"),
                "PEN",
                PaymentStatus.PENDING));
        paymentRepository.saveAndFlush(new Payment(
                otherMerchant,
                new BigDecimal("75.00"),
                "USD",
                PaymentStatus.PENDING));

        List<Payment> visiblePayments = paymentService.getAllPayments();

        assertThat(visiblePayments).extracting(Payment::getId)
                .containsExactly(ownedPayment.getId());
    }

    @Test
    void hidesPaymentOwnedByAnotherMerchant() {
        Merchant otherMerchant = merchantRepository.saveAndFlush(
                new Merchant("Other Merchant", MerchantStatus.ACTIVE));
        Payment otherPayment = paymentRepository.saveAndFlush(new Payment(
                otherMerchant,
                new BigDecimal("75.00"),
                "USD",
                PaymentStatus.PENDING));

        assertThatThrownBy(() -> paymentService.getPaymentById(otherPayment.getId()))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessage("Payment with id " + otherPayment.getId() + " was not found");
    }

    private CreatePaymentRequest paymentRequest(String amount, String currency) {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setAmount(new BigDecimal(amount));
        request.setCurrency(currency);
        return request;
    }
}
