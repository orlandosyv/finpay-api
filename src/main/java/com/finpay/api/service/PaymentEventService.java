package com.finpay.api.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.finpay.api.model.OutboxEvent;
import com.finpay.api.model.Payment;
import com.finpay.api.model.WebhookEventType;
import com.finpay.api.repository.OutboxEventRepository;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class PaymentEventService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public PaymentEventService(
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public void record(Payment payment, WebhookEventType eventType) {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();
        String payload = serializePayload(eventId, occurredAt, payment, eventType);
        outboxEventRepository.save(new OutboxEvent(
                eventId,
                payment.getMerchant(),
                payment.getId(),
                eventType,
                payload,
                occurredAt));
    }

    private String serializePayload(
            UUID eventId,
            Instant occurredAt,
            Payment payment,
            WebhookEventType eventType) {
        Map<String, Object> paymentData = new LinkedHashMap<>();
        paymentData.put("id", payment.getId());
        paymentData.put("amount", payment.getAmount());
        paymentData.put("currency", payment.getCurrency());
        paymentData.put("status", payment.getStatus());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("payment", paymentData);

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("id", eventId);
        event.put("type", eventType.getEventName());
        event.put("createdAt", occurredAt);
        event.put("data", data);

        try {
            return objectMapper.writeValueAsString(event);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Payment event could not be serialized", exception);
        }
    }
}
