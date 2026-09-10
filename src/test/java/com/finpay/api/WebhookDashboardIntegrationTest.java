package com.finpay.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.finpay.api.config.TestcontainersConfiguration;
import com.finpay.api.model.Merchant;
import com.finpay.api.model.MerchantStatus;
import com.finpay.api.model.OutboxEvent;
import com.finpay.api.model.WebhookDelivery;
import com.finpay.api.model.WebhookDeliveryStatus;
import com.finpay.api.model.WebhookEndpoint;
import com.finpay.api.model.WebhookEventType;
import com.finpay.api.repository.MerchantRepository;
import com.finpay.api.repository.OutboxEventRepository;
import com.finpay.api.repository.WebhookDeliveryRepository;
import com.finpay.api.repository.WebhookEndpointRepository;

@SpringBootTest(properties = "finpay.webhook.scheduler-enabled=false")
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class WebhookDashboardIntegrationTest {

    private static final Long DEFAULT_MERCHANT_ID = 1L;

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private WebhookEndpointRepository webhookEndpointRepository;

    @Autowired
    private WebhookDeliveryRepository webhookDeliveryRepository;

    private MockMvc mockMvc;
    private UUID processedEventId;
    private UUID failedEventId;
    private UUID otherMerchantEventId;

    @BeforeEach
    void setUp() {
        webhookDeliveryRepository.deleteAll();
        outboxEventRepository.deleteAll();
        webhookEndpointRepository.deleteAll();

        Merchant merchant = merchantRepository.findById(DEFAULT_MERCHANT_ID).orElseThrow();
        Merchant otherMerchant = merchantRepository.saveAndFlush(
                new Merchant("Dashboard Other Merchant", MerchantStatus.ACTIVE));

        WebhookEndpoint endpoint = webhookEndpointRepository.saveAndFlush(
                new WebhookEndpoint(
                        merchant,
                        "https://merchant.example.com/webhooks/finpay",
                        "encrypted-secret"));

        OutboxEvent processedEvent = new OutboxEvent(
                UUID.randomUUID(),
                merchant,
                101L,
                WebhookEventType.PAYMENT_CREATED,
                payload("payment.created", 101L, "PENDING"),
                Instant.now());
        processedEvent.markProcessed(Instant.now());
        processedEvent = outboxEventRepository.saveAndFlush(processedEvent);
        processedEventId = processedEvent.getId();

        webhookDeliveryRepository.saveAndFlush(new WebhookDelivery(
                processedEvent,
                endpoint,
                1,
                WebhookDeliveryStatus.SUCCEEDED,
                204,
                null,
                Instant.now()));

        OutboxEvent failedEvent = new OutboxEvent(
                UUID.randomUUID(),
                merchant,
                102L,
                WebhookEventType.PAYMENT_REFUNDED,
                payload("payment.refunded", 102L, "REFUNDED"),
                Instant.now());
        failedEvent.markFailed();
        failedEvent = outboxEventRepository.saveAndFlush(failedEvent);
        failedEventId = failedEvent.getId();

        webhookDeliveryRepository.saveAndFlush(new WebhookDelivery(
                failedEvent,
                endpoint,
                1,
                WebhookDeliveryStatus.FAILED,
                500,
                "Webhook returned HTTP 500",
                Instant.now()));

        OutboxEvent otherEvent = outboxEventRepository.saveAndFlush(new OutboxEvent(
                UUID.randomUUID(),
                otherMerchant,
                999L,
                WebhookEventType.PAYMENT_APPROVED,
                payload("payment.approved", 999L, "APPROVED"),
                Instant.now()));
        otherMerchantEventId = otherEvent.getId();

        mockMvc = MockMvcBuilders
                .webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void returnsTenantScopedSummaryFilteredPageDetailAndDeliveries() throws Exception {
        mockMvc.perform(get("/api/merchant/webhook-events/summary")
                        .with(merchantJwt(DEFAULT_MERCHANT_ID, "MERCHANT_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEvents").value(2))
                .andExpect(jsonPath("$.processedEvents").value(1))
                .andExpect(jsonPath("$.pendingEvents").value(0))
                .andExpect(jsonPath("$.failedEvents").value(1))
                .andExpect(jsonPath("$.processedRate").value(50.00))
                .andExpect(jsonPath("$.totalDeliveries").value(2))
                .andExpect(jsonPath("$.successfulDeliveries").value(1))
                .andExpect(jsonPath("$.failedDeliveries").value(1))
                .andExpect(jsonPath("$.deliverySuccessRate").value(50.00));

        mockMvc.perform(get("/api/merchant/webhook-events")
                        .param("page", "0")
                        .param("size", "1")
                        .with(merchantJwt(DEFAULT_MERCHANT_ID, "MERCHANT_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get("/api/merchant/webhook-events")
                        .param("status", "FAILED")
                        .param("eventType", "PAYMENT_REFUNDED")
                        .param("paymentId", "102")
                        .with(merchantJwt(DEFAULT_MERCHANT_ID, "MERCHANT_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].eventId")
                        .value(failedEventId.toString()))
                .andExpect(jsonPath("$.content[0].paymentId").value(102))
                .andExpect(jsonPath("$.content[0].eventType")
                        .value("payment.refunded"))
                .andExpect(jsonPath("$.content[0].status").value("FAILED"));

        mockMvc.perform(get("/api/merchant/webhook-events/{eventId}", processedEventId)
                        .with(merchantJwt(DEFAULT_MERCHANT_ID, "MERCHANT_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(processedEventId.toString()))
                .andExpect(jsonPath("$.payload.type").value("payment.created"))
                .andExpect(jsonPath("$.payload.data.payment.id").value(101));

        mockMvc.perform(get(
                        "/api/merchant/webhook-events/{eventId}/deliveries",
                        failedEventId)
                        .with(merchantJwt(DEFAULT_MERCHANT_ID, "MERCHANT_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].eventId").value(failedEventId.toString()))
                .andExpect(jsonPath("$[0].attemptNumber").value(1))
                .andExpect(jsonPath("$[0].status").value("FAILED"))
                .andExpect(jsonPath("$[0].responseStatus").value(500))
                .andExpect(jsonPath("$[0].errorMessage")
                        .value("Webhook returned HTTP 500"));
    }

    @Test
    void hidesOtherMerchantEventsAndRequiresAdminRole() throws Exception {
        mockMvc.perform(get("/api/merchant/webhook-events/{eventId}", otherMerchantEventId)
                        .with(merchantJwt(DEFAULT_MERCHANT_ID, "MERCHANT_ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(
                        "Webhook event with id " + otherMerchantEventId + " was not found"));

        mockMvc.perform(get("/api/merchant/webhook-events")
                        .with(merchantJwt(DEFAULT_MERCHANT_ID, "MERCHANT_USER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/merchant/webhook-events"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidDashboardFilters() throws Exception {
        mockMvc.perform(get("/api/merchant/webhook-events")
                        .param("size", "101")
                        .with(merchantJwt(DEFAULT_MERCHANT_ID, "MERCHANT_ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Size must be between 1 and 100"));

        mockMvc.perform(get("/api/merchant/webhook-events/summary")
                        .param("from", "2026-09-10T20:00:00Z")
                        .param("to", "2026-09-10T19:00:00Z")
                        .with(merchantJwt(DEFAULT_MERCHANT_ID, "MERCHANT_ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("From must be before or equal to to"));
    }

    private RequestPostProcessor merchantJwt(Long merchantId, String role) {
        return jwt().jwt(token -> token
                        .subject("1")
                        .claim("merchantId", merchantId)
                        .claim("roles", List.of(role)))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private String payload(String type, Long paymentId, String paymentStatus) {
        return """
                {"type":"%s","data":{"payment":{"id":%d,"status":"%s"}}}
                """.formatted(type, paymentId, paymentStatus).trim();
    }
}
