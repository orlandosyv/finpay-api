package com.finpay.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
                .defaultRequest(get("/").with(jwt().jwt(token -> token
                        .subject("1")
                        .claim("merchantId", 1L)
                        .claim("roles", List.of("MERCHANT_ADMIN")))
                        .authorities(new SimpleGrantedAuthority("ROLE_MERCHANT_ADMIN"))))
                .apply(springSecurity())
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

    @Test
    void exposesOpenApiDocumentationAndSwaggerUi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("FinPay API"))
                .andExpect(jsonPath("$.info.version").value("1.0.0"))
                .andExpect(jsonPath("$.paths['/api/health']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/register']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/login']").exists())
                .andExpect(jsonPath("$.paths['/api/merchant/me']").exists())
                .andExpect(jsonPath("$.paths['/api/merchant/users']").exists())
                .andExpect(jsonPath("$.paths['/api/payments']").exists())
                .andExpect(jsonPath("$.paths['/api/payments/{id}']").exists())
                .andExpect(jsonPath("$.paths['/api/payments/{id}/approve']").exists())
                .andExpect(jsonPath("$.paths['/api/payments/{id}/decline']").exists())
                .andExpect(jsonPath("$.paths['/api/payments/{id}/refund']").exists())
                .andExpect(jsonPath("$.paths['/api/payments'].get.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/payments'].get.responses['403']").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth").exists());

        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
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
