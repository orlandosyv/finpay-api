package com.finpay.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.AfterEach;
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
import com.finpay.api.model.OutboxEventStatus;
import com.finpay.api.repository.MerchantRepository;
import com.finpay.api.repository.OutboxEventRepository;
import com.finpay.api.repository.PaymentIdempotencyRepository;
import com.finpay.api.repository.PaymentRepository;
import com.finpay.api.repository.WebhookDeliveryRepository;
import com.finpay.api.repository.WebhookEndpointRepository;
import com.finpay.api.service.WebhookDispatchService;
import com.finpay.api.service.WebhookSecretCrypto;
import com.jayway.jsonpath.JsonPath;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

@SpringBootTest(properties = "finpay.webhook.scheduler-enabled=false")
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class WebhookFlowIntegrationTest {

    private static final Long DEFAULT_MERCHANT_ID = 1L;

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private WebhookDispatchService dispatchService;

    @Autowired
    private WebhookEndpointRepository webhookEndpointRepository;

    @Autowired
    private WebhookDeliveryRepository webhookDeliveryRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private PaymentIdempotencyRepository idempotencyRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private WebhookSecretCrypto secretCrypto;

    private final BlockingQueue<ReceivedWebhook> receivedWebhooks =
            new LinkedBlockingQueue<>();
    private final AtomicInteger receiverStatus = new AtomicInteger(204);

    private MockMvc mockMvc;
    private HttpServer receiver;
    private String receiverUrl;

    @BeforeEach
    void setUp() throws IOException {
        webhookDeliveryRepository.deleteAll();
        outboxEventRepository.deleteAll();
        webhookEndpointRepository.deleteAll();
        idempotencyRepository.deleteAll();
        paymentRepository.deleteAll();
        receivedWebhooks.clear();
        receiverStatus.set(204);

        receiver = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        receiver.createContext("/finpay", this::receiveWebhook);
        receiver.start();
        receiverUrl = "http://127.0.0.1:"
                + receiver.getAddress().getPort()
                + "/finpay";

        mockMvc = MockMvcBuilders
                .webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @AfterEach
    void tearDown() {
        if (receiver != null) {
            receiver.stop(0);
        }
    }

    @Test
    void deliversSignedCreatedAndApprovedEvents() throws Exception {
        CreatedEndpoint endpoint = createEndpoint(DEFAULT_MERCHANT_ID, receiverUrl);
        assertThat(webhookEndpointRepository.findById(endpoint.id()).orElseThrow()
                .getSecretEncrypted()).doesNotContain(endpoint.signingSecret());
        assertThat(secretCrypto.decrypt(webhookEndpointRepository
                .findById(endpoint.id()).orElseThrow().getSecretEncrypted()))
                .isEqualTo(endpoint.signingSecret());

        MvcResult paymentResult = mockMvc.perform(post("/api/payments")
                        .with(merchantJwt(DEFAULT_MERCHANT_ID, "MERCHANT_ADMIN"))
                        .header("Idempotency-Key", "webhook-payment-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 135.50,
                                  "currency": "PEN"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        Number paymentId = JsonPath.read(
                paymentResult.getResponse().getContentAsString(),
                "$.id");

        assertThat(outboxEventRepository.findAll())
                .singleElement()
                .satisfies(event -> assertThat(event.getStatus())
                        .isEqualTo(OutboxEventStatus.PENDING));

        dispatchService.dispatchPendingEvents();
        ReceivedWebhook created = receivedWebhooks.poll(5, TimeUnit.SECONDS);
        assertValidWebhook(created, endpoint.signingSecret(), "payment.created", "PENDING");

        mockMvc.perform(patch("/api/payments/{id}/approve", paymentId.longValue())
                        .with(merchantJwt(DEFAULT_MERCHANT_ID, "MERCHANT_ADMIN")))
                .andExpect(status().isOk());

        dispatchService.dispatchPendingEvents();
        ReceivedWebhook approved = receivedWebhooks.poll(5, TimeUnit.SECONDS);
        assertValidWebhook(approved, endpoint.signingSecret(), "payment.approved", "APPROVED");

        assertThat(outboxEventRepository.findAll())
                .allSatisfy(event -> assertThat(event.getStatus())
                        .isEqualTo(OutboxEventStatus.PROCESSED));
        assertThat(webhookDeliveryRepository.count()).isEqualTo(2);
    }

    @Test
    void managesEndpointsPerMerchantAndRequiresAdminRole() throws Exception {
        mockMvc.perform(post("/api/merchant/webhooks")
                        .with(merchantJwt(DEFAULT_MERCHANT_ID, "MERCHANT_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"" + receiverUrl + "\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/merchant/webhooks")
                        .with(merchantJwt(DEFAULT_MERCHANT_ID, "MERCHANT_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"ftp://example.com/hook\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Webhook URL must use HTTP or HTTPS"));

        CreatedEndpoint endpoint = createEndpoint(DEFAULT_MERCHANT_ID, receiverUrl);

        mockMvc.perform(get("/api/merchant/webhooks")
                        .with(merchantJwt(DEFAULT_MERCHANT_ID, "MERCHANT_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(endpoint.id()))
                .andExpect(jsonPath("$[0].signingSecret").doesNotExist());

        Merchant otherMerchant = merchantRepository.saveAndFlush(
                new Merchant("Other Webhook Merchant", MerchantStatus.ACTIVE));
        mockMvc.perform(get("/api/merchant/webhooks")
                        .with(merchantJwt(otherMerchant.getId(), "MERCHANT_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(delete("/api/merchant/webhooks/{id}", endpoint.id())
                        .with(merchantJwt(otherMerchant.getId(), "MERCHANT_ADMIN")))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/merchant/webhooks/{id}", endpoint.id())
                        .with(merchantJwt(DEFAULT_MERCHANT_ID, "MERCHANT_ADMIN")))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/merchant/webhooks")
                        .with(merchantJwt(DEFAULT_MERCHANT_ID, "MERCHANT_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void recordsFailedDeliveryAndSchedulesRetry() throws Exception {
        createEndpoint(DEFAULT_MERCHANT_ID, receiverUrl);
        receiverStatus.set(500);

        mockMvc.perform(post("/api/payments")
                        .with(merchantJwt(DEFAULT_MERCHANT_ID, "MERCHANT_ADMIN"))
                        .header("Idempotency-Key", "webhook-failure-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":40.00,\"currency\":\"USD\"}"))
                .andExpect(status().isCreated());

        dispatchService.dispatchPendingEvents();

        assertThat(receivedWebhooks.poll(5, TimeUnit.SECONDS)).isNotNull();
        assertThat(webhookDeliveryRepository.count()).isEqualTo(1);
        assertThat(outboxEventRepository.findAll())
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
                    assertThat(event.getAttempts()).isEqualTo(1);
                    assertThat(event.getAvailableAt()).isAfter(java.time.Instant.now());
                });
    }

    private CreatedEndpoint createEndpoint(Long merchantId, String url) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/merchant/webhooks")
                        .with(merchantJwt(merchantId, "MERCHANT_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"" + url + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.signingSecret").isNotEmpty())
                .andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        String secret = JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.signingSecret");
        return new CreatedEndpoint(id.longValue(), secret);
    }

    private RequestPostProcessor merchantJwt(Long merchantId, String role) {
        return jwt().jwt(token -> token
                        .subject("1")
                        .claim("merchantId", merchantId)
                        .claim("roles", List.of(role)))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private void receiveWebhook(HttpExchange exchange) throws IOException {
        String body = new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8);
        receivedWebhooks.add(new ReceivedWebhook(
                exchange.getRequestHeaders().getFirst("X-FinPay-Event-Id"),
                exchange.getRequestHeaders().getFirst("X-FinPay-Event-Type"),
                exchange.getRequestHeaders().getFirst("X-FinPay-Timestamp"),
                exchange.getRequestHeaders().getFirst("X-FinPay-Signature"),
                body));
        int status = receiverStatus.get();
        exchange.sendResponseHeaders(status, -1);
        exchange.close();
    }

    private void assertValidWebhook(
            ReceivedWebhook webhook,
            String secret,
            String expectedType,
            String expectedPaymentStatus) throws Exception {
        assertThat(webhook).isNotNull();
        assertThat(webhook.eventId()).isNotBlank();
        assertThat(webhook.eventType()).isEqualTo(expectedType);
        assertThat(JsonPath.<String>read(webhook.body(), "$.type"))
                .isEqualTo(expectedType);
        assertThat(JsonPath.<String>read(webhook.body(), "$.data.payment.status"))
                .isEqualTo(expectedPaymentStatus);
        assertThat(webhook.signature()).isEqualTo(
                independentlySign(secret, webhook.timestamp(), webhook.body()));
    }

    private String independentlySign(String secret, String timestamp, String body)
            throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "v1=" + HexFormat.of().formatHex(mac.doFinal(
                (timestamp + "." + body).getBytes(StandardCharsets.UTF_8)));
    }

    private record CreatedEndpoint(Long id, String signingSecret) {
    }

    private record ReceivedWebhook(
            String eventId,
            String eventType,
            String timestamp,
            String signature,
            String body) {
    }
}
