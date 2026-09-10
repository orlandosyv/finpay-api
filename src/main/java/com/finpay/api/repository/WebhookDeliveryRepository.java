package com.finpay.api.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.finpay.api.model.WebhookDelivery;
import com.finpay.api.model.WebhookDeliveryStatus;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {

    boolean existsByEventIdAndWebhookEndpointIdAndStatus(
            UUID eventId,
            Long webhookEndpointId,
            WebhookDeliveryStatus status);
}
