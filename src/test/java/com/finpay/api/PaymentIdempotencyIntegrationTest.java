package com.finpay.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.finpay.api.config.TestcontainersConfiguration;
import com.finpay.api.model.Merchant;
import com.finpay.api.model.MerchantStatus;
import com.finpay.api.repository.MerchantRepository;
import com.finpay.api.repository.PaymentIdempotencyRepository;
import com.finpay.api.repository.PaymentRepository;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class PaymentIdempotencyIntegrationTest {

    private static final Long DEFAULT_MERCHANT_ID = 1L;

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentIdempotencyRepository idempotencyRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        idempotencyRepository.deleteAll();
        paymentRepository.deleteAll();
        mockMvc = MockMvcBuilders
                .webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void replaysOriginalCreationResponseWithoutCreatingAnotherPayment() throws Exception {
        MvcResult firstResponse = createPayment(
                DEFAULT_MERCHANT_ID,
                "retry-safe-payment",
                "150.00",
                "PEN")
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotency-Replayed", "false"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        Number originalId = JsonPath.read(
                firstResponse.getResponse().getContentAsString(),
                "$.id");

        MvcResult replayedResponse = createPayment(
                DEFAULT_MERCHANT_ID,
                "retry-safe-payment",
                "150",
                "PEN")
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotency-Replayed", "true"))
                .andExpect(jsonPath("$.id").value(originalId.longValue()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        assertThat(replayedResponse.getResponse().getContentAsString())
                .isEqualTo(firstResponse.getResponse().getContentAsString());
        assertThat(paymentRepository.count()).isEqualTo(1);
        assertThat(idempotencyRepository.count()).isEqualTo(1);
    }

    @Test
    void rejectsReuseOfKeyWithDifferentPaymentData() throws Exception {
        createPayment(DEFAULT_MERCHANT_ID, "conflicting-payment", "20.00", "USD")
                .andExpect(status().isCreated());

        createPayment(DEFAULT_MERCHANT_ID, "conflicting-payment", "21.00", "USD")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(
                        "Idempotency-Key was already used with a different payment request"));

        assertThat(paymentRepository.count()).isEqualTo(1);
    }

    @Test
    void requiresAValidIdempotencyKey() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .with(merchantJwt(DEFAULT_MERCHANT_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentJson("25.00", "PEN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Idempotency-Key header is required"));

        createPayment(DEFAULT_MERCHANT_ID, " ", "25.00", "PEN")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Idempotency-Key header is required"));

        createPayment(DEFAULT_MERCHANT_ID, "x".repeat(129), "25.00", "PEN")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Idempotency-Key must not exceed 128 characters"));

        assertThat(paymentRepository.count()).isZero();
    }

    @Test
    void scopesTheSameKeyIndependentlyForEachMerchant() throws Exception {
        Merchant secondMerchant = merchantRepository.saveAndFlush(
                new Merchant("Second Idempotency Merchant", MerchantStatus.ACTIVE));

        createPayment(DEFAULT_MERCHANT_ID, "shared-client-key", "30.00", "EUR")
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotency-Replayed", "false"));

        createPayment(secondMerchant.getId(), "shared-client-key", "30.00", "EUR")
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotency-Replayed", "false"));

        assertThat(paymentRepository.count()).isEqualTo(2);
        assertThat(idempotencyRepository.count()).isEqualTo(2);
    }

    @Test
    void handlesConcurrentRetriesWithoutDuplicatingThePayment() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<MvcResult> first = executor.submit(() -> concurrentCreate(ready, start));
            Future<MvcResult> second = executor.submit(() -> concurrentCreate(ready, start));

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            MvcResult firstResult = first.get(30, TimeUnit.SECONDS);
            MvcResult secondResult = second.get(30, TimeUnit.SECONDS);

            assertThat(firstResult.getResponse().getStatus()).isEqualTo(201);
            assertThat(secondResult.getResponse().getStatus()).isEqualTo(201);
            assertThat(List.of(
                    firstResult.getResponse().getHeader("Idempotency-Replayed"),
                    secondResult.getResponse().getHeader("Idempotency-Replayed")))
                    .containsExactlyInAnyOrder("false", "true");

            Number firstId = JsonPath.read(firstResult.getResponse().getContentAsString(), "$.id");
            Number secondId = JsonPath.read(secondResult.getResponse().getContentAsString(), "$.id");
            assertThat(firstId.longValue()).isEqualTo(secondId.longValue());
        }

        assertThat(paymentRepository.count()).isEqualTo(1);
        assertThat(idempotencyRepository.count()).isEqualTo(1);
    }

    private MvcResult concurrentCreate(
            CountDownLatch ready,
            CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent test did not start in time");
        }
        return createPayment(
                DEFAULT_MERCHANT_ID,
                "concurrent-payment",
                "75.00",
                "PEN")
                .andReturn();
    }

    private org.springframework.test.web.servlet.ResultActions createPayment(
            Long merchantId,
            String key,
            String amount,
            String currency) throws Exception {
        return mockMvc.perform(post("/api/payments")
                .with(merchantJwt(merchantId))
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentJson(amount, currency)));
    }

    private RequestPostProcessor merchantJwt(Long merchantId) {
        return jwt().jwt(token -> token
                        .subject("1")
                        .claim("merchantId", merchantId)
                        .claim("roles", List.of("MERCHANT_ADMIN")))
                .authorities(new SimpleGrantedAuthority("ROLE_MERCHANT_ADMIN"));
    }

    private String paymentJson(String amount, String currency) {
        return """
                {
                  "amount": %s,
                  "currency": "%s"
                }
                """.formatted(amount, currency);
    }
}
