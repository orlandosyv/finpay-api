package com.finpay.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finpay.api.context.CurrentMerchantProvider;
import com.finpay.api.dto.PagedWebhookEventResponse;
import com.finpay.api.dto.WebhookDashboardSummaryResponse;
import com.finpay.api.dto.WebhookDeliveryResponse;
import com.finpay.api.dto.WebhookEventDetailResponse;
import com.finpay.api.dto.WebhookEventSummaryResponse;
import com.finpay.api.exception.InvalidWebhookDashboardFilterException;
import com.finpay.api.exception.WebhookEventNotFoundException;
import com.finpay.api.model.OutboxEvent;
import com.finpay.api.model.OutboxEventStatus;
import com.finpay.api.model.WebhookDelivery;
import com.finpay.api.model.WebhookDeliveryStatus;
import com.finpay.api.model.WebhookEventType;
import com.finpay.api.repository.OutboxEventRepository;
import com.finpay.api.repository.WebhookDeliveryRepository;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class WebhookDashboardService {

    private static final int MAX_PAGE_SIZE = 100;

    private final CurrentMerchantProvider currentMerchantProvider;
    private final OutboxEventRepository outboxEventRepository;
    private final WebhookDeliveryRepository webhookDeliveryRepository;
    private final ObjectMapper objectMapper;

    public WebhookDashboardService(
            CurrentMerchantProvider currentMerchantProvider,
            OutboxEventRepository outboxEventRepository,
            WebhookDeliveryRepository webhookDeliveryRepository,
            ObjectMapper objectMapper) {
        this.currentMerchantProvider = currentMerchantProvider;
        this.outboxEventRepository = outboxEventRepository;
        this.webhookDeliveryRepository = webhookDeliveryRepository;
        this.objectMapper = objectMapper;
    }

    @PreAuthorize("hasRole('MERCHANT_ADMIN')")
    @Transactional(readOnly = true)
    public PagedWebhookEventResponse getEvents(
            OutboxEventStatus status,
            WebhookEventType eventType,
            Long paymentId,
            Instant from,
            Instant to,
            int page,
            int size) {
        validateFilters(from, to, page, size);
        Long merchantId = currentMerchantProvider.getCurrentMerchantId();
        Specification<OutboxEvent> specification = filters(
                merchantId,
                status,
                eventType,
                paymentId,
                from,
                to);
        Page<OutboxEvent> events = outboxEventRepository.findAll(
                specification,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        return new PagedWebhookEventResponse(
                events.getContent().stream().map(this::toSummary).toList(),
                events.getNumber(),
                events.getSize(),
                events.getTotalElements(),
                events.getTotalPages());
    }

    @PreAuthorize("hasRole('MERCHANT_ADMIN')")
    @Transactional(readOnly = true)
    public WebhookDashboardSummaryResponse getSummary(Instant from, Instant to) {
        validateDateRange(from, to);
        Long merchantId = currentMerchantProvider.getCurrentMerchantId();
        Specification<OutboxEvent> base = filters(
                merchantId,
                null,
                null,
                null,
                from,
                to);
        long total = outboxEventRepository.count(base);
        long processed = outboxEventRepository.count(base.and(hasStatus(
                OutboxEventStatus.PROCESSED)));
        long pending = outboxEventRepository.count(base.and(hasStatus(
                OutboxEventStatus.PENDING)));
        long failed = outboxEventRepository.count(base.and(hasStatus(
                OutboxEventStatus.FAILED)));

        long totalDeliveries = webhookDeliveryRepository.countForDashboard(
                merchantId,
                null,
                from,
                to);
        long successfulDeliveries = webhookDeliveryRepository.countForDashboard(
                merchantId,
                WebhookDeliveryStatus.SUCCEEDED,
                from,
                to);
        long failedDeliveries = webhookDeliveryRepository.countForDashboard(
                merchantId,
                WebhookDeliveryStatus.FAILED,
                from,
                to);

        BigDecimal processedRate = total == 0
                ? BigDecimal.ZERO.setScale(2)
                : percentage(processed, total);
        BigDecimal deliverySuccessRate = totalDeliveries == 0
                ? BigDecimal.ZERO.setScale(2)
                : percentage(successfulDeliveries, totalDeliveries);

        return new WebhookDashboardSummaryResponse(
                total,
                processed,
                pending,
                failed,
                processedRate,
                totalDeliveries,
                successfulDeliveries,
                failedDeliveries,
                deliverySuccessRate,
                from,
                to);
    }

    @PreAuthorize("hasRole('MERCHANT_ADMIN')")
    @Transactional(readOnly = true)
    public WebhookEventDetailResponse getEvent(UUID eventId) {
        OutboxEvent event = findMerchantEvent(eventId);
        return new WebhookEventDetailResponse(
                event.getId(),
                event.getAggregateId(),
                event.getEventType().getEventName(),
                event.getStatus(),
                event.getAttempts(),
                event.getCreatedAt(),
                nextAttemptAt(event),
                event.getProcessedAt(),
                parsePayload(event.getPayload()));
    }

    @PreAuthorize("hasRole('MERCHANT_ADMIN')")
    @Transactional(readOnly = true)
    public List<WebhookDeliveryResponse> getDeliveries(UUID eventId) {
        Long merchantId = currentMerchantProvider.getCurrentMerchantId();
        if (outboxEventRepository.findByIdAndMerchantId(eventId, merchantId).isEmpty()) {
            throw new WebhookEventNotFoundException(eventId);
        }
        return webhookDeliveryRepository.findAllByEventAndMerchant(eventId, merchantId)
                .stream()
                .map(this::toDeliveryResponse)
                .toList();
    }

    private OutboxEvent findMerchantEvent(UUID eventId) {
        return outboxEventRepository.findByIdAndMerchantId(
                        eventId,
                        currentMerchantProvider.getCurrentMerchantId())
                .orElseThrow(() -> new WebhookEventNotFoundException(eventId));
    }

    private Specification<OutboxEvent> filters(
            Long merchantId,
            OutboxEventStatus status,
            WebhookEventType eventType,
            Long paymentId,
            Instant from,
            Instant to) {
        Specification<OutboxEvent> specification = (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("merchant").get("id"), merchantId);
        if (status != null) {
            specification = specification.and(hasStatus(status));
        }
        if (eventType != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("eventType"), eventType));
        }
        if (paymentId != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("aggregateId"), paymentId));
        }
        if (from != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (to != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), to));
        }
        return specification;
    }

    private Specification<OutboxEvent> hasStatus(OutboxEventStatus status) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("status"), status);
    }

    private WebhookEventSummaryResponse toSummary(OutboxEvent event) {
        return new WebhookEventSummaryResponse(
                event.getId(),
                event.getAggregateId(),
                event.getEventType().getEventName(),
                event.getStatus(),
                event.getAttempts(),
                event.getCreatedAt(),
                nextAttemptAt(event),
                event.getProcessedAt());
    }

    private WebhookDeliveryResponse toDeliveryResponse(WebhookDelivery delivery) {
        return new WebhookDeliveryResponse(
                delivery.getId(),
                delivery.getEventId(),
                delivery.getWebhookEndpointId(),
                delivery.getWebhookEndpointUrl(),
                delivery.getAttemptNumber(),
                delivery.getStatus(),
                delivery.getResponseStatus(),
                delivery.getErrorMessage(),
                delivery.getAttemptedAt());
    }

    private Instant nextAttemptAt(OutboxEvent event) {
        return event.getStatus() == OutboxEventStatus.PENDING
                ? event.getAvailableAt()
                : null;
    }

    private JsonNode parsePayload(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Stored webhook payload is invalid", exception);
        }
    }

    private void validateFilters(Instant from, Instant to, int page, int size) {
        validateDateRange(from, to);
        if (page < 0) {
            throw new InvalidWebhookDashboardFilterException(
                    "Page must be zero or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidWebhookDashboardFilterException(
                    "Size must be between 1 and " + MAX_PAGE_SIZE);
        }
    }

    private void validateDateRange(Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidWebhookDashboardFilterException(
                    "From must be before or equal to to");
        }
    }

    private BigDecimal percentage(long numerator, long denominator) {
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }
}
