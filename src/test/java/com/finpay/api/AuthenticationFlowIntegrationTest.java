package com.finpay.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.jayway.jsonpath.JsonPath;
import com.finpay.api.config.TestcontainersConfiguration;
import com.finpay.api.model.Payment;
import com.finpay.api.repository.PaymentRepository;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class AuthenticationFlowIntegrationTest {

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private PaymentRepository paymentRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void authenticatesAndIsolatesPaymentsBetweenMerchants() throws Exception {
        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message")
                        .value("Authentication is required or the access token is invalid"))
                .andExpect(jsonPath("$.path").value("/api/payments"));

        RegistrationData firstMerchant = register(
                "JWT Shop One",
                "jwt-owner-one@finpay.test",
                "StrongPassword123!");
        String firstToken = login(
                "jwt-owner-one@finpay.test",
                "StrongPassword123!");

        MvcResult createResult = mockMvc.perform(post("/api/payments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(firstToken))
                        .header("Idempotency-Key", "authentication-flow-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 150.00,
                                  "currency": "PEN"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        Number paymentIdValue = JsonPath.read(
                createResult.getResponse().getContentAsString(),
                "$.id");
        Long paymentId = paymentIdValue.longValue();
        Payment storedPayment = paymentRepository.findById(paymentId).orElseThrow();
        assertThat(storedPayment.getMerchant().getId()).isEqualTo(firstMerchant.merchantId());

        register(
                "JWT Shop Two",
                "jwt-owner-two@finpay.test",
                "AnotherPassword456!");
        String secondToken = login(
                "jwt-owner-two@finpay.test",
                "AnotherPassword456!");

        mockMvc.perform(get("/api/payments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(secondToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(get("/api/payments/{id}", paymentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(secondToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/payments/{id}", paymentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(firstToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId));
    }

    @Test
    void rejectsWrongPasswordAndTamperedToken() throws Exception {
        register(
                "JWT Security Shop",
                "jwt-security@finpay.test",
                "StrongPassword123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "jwt-security@finpay.test",
                                  "password": "WrongPassword123!"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        String token = login("jwt-security@finpay.test", "StrongPassword123!");

        mockMvc.perform(get("/api/payments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token + "tampered")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message")
                        .value("Authentication is required or the access token is invalid"));
    }

    private RegistrationData register(
            String merchantName,
            String email,
            String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "merchantName": "%s",
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(merchantName, email, password)))
                .andExpect(status().isCreated())
                .andReturn();

        Number merchantId = JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.merchantId");
        return new RegistrationData(merchantId.longValue());
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshExpiresIn").value(604800))
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record RegistrationData(Long merchantId) {
    }
}
