package com.finpay.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.finpay.api.config.TestcontainersConfiguration;
import com.finpay.api.model.Payment;
import com.finpay.api.model.PaymentStatus;
import com.finpay.api.repository.PaymentRepository;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class PaymentFlowIntegrationTest {

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private PaymentRepository paymentRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        mockMvc = MockMvcBuilders
                .webAppContextSetup(applicationContext)
                .build();
    }

    @Test
    void createsFindsApprovesAndRefundsPayment() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 125.50,
                                  "currency": "PEN"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(125.50))
                .andExpect(jsonPath("$.currency").value("PEN"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        Payment createdPayment = onlyStoredPayment();
        Long paymentId = createdPayment.getId();

        mockMvc.perform(get("/api/payments/{id}", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId))
                .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(patch("/api/payments/{id}/approve", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        assertThat(storedPayment(paymentId).getStatus())
                .isEqualTo(PaymentStatus.APPROVED);

        mockMvc.perform(patch("/api/payments/{id}/refund", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));

        mockMvc.perform(get("/api/payments/{id}", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));

        assertThat(storedPayment(paymentId).getStatus())
                .isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void createsAndDeclinesPaymentAndRejectsRefund() throws Exception {
        createPayment("80.00", "USD");
        Long paymentId = onlyStoredPayment().getId();

        mockMvc.perform(patch("/api/payments/{id}/decline", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECLINED"));

        mockMvc.perform(patch("/api/payments/{id}/refund", paymentId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("Payment cannot transition from DECLINED to REFUNDED"));

        assertThat(storedPayment(paymentId).getStatus())
                .isEqualTo(PaymentStatus.DECLINED);
    }

    @Test
    void returnsValidationAndNotFoundErrors() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": -10.00,
                                  "currency": "usd"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors.amount")
                        .value("Amount must be greater than zero"))
                .andExpect(jsonPath("$.fieldErrors.currency")
                        .value("Currency must be a valid uppercase ISO 4217 code"));

        assertThat(paymentRepository.count()).isZero();

        mockMvc.perform(get("/api/payments/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("Payment with id " + Long.MAX_VALUE + " was not found"));
    }

    private void createPayment(String amount, String currency) throws Exception {
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": %s,
                                  "currency": "%s"
                                }
                                """.formatted(amount, currency)))
                .andExpect(status().isCreated());
    }

    private Payment onlyStoredPayment() {
        List<Payment> payments = paymentRepository.findAll();
        assertThat(payments).hasSize(1);
        return payments.getFirst();
    }

    private Payment storedPayment(Long paymentId) {
        return paymentRepository.findById(paymentId).orElseThrow();
    }
}
