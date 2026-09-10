package com.finpay.api.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import com.finpay.api.model.WebhookDelivery;
import com.finpay.api.model.WebhookDeliveryStatus;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {

    boolean existsByEvent_IdAndWebhookEndpoint_IdAndStatus(
            UUID eventId,
            Long webhookEndpointId,
            WebhookDeliveryStatus status);

    @Query("""
            select delivery
            from WebhookDelivery delivery
            join fetch delivery.webhookEndpoint endpoint
            where delivery.event.id = :eventId
              and delivery.event.merchant.id = :merchantId
            order by delivery.attemptedAt asc
            """)
    List<WebhookDelivery> findAllByEventAndMerchant(
            @Param("eventId") UUID eventId,
            @Param("merchantId") Long merchantId);

    @Query("""
            select count(delivery)
            from WebhookDelivery delivery
            where delivery.event.merchant.id = :merchantId
              and (:status is null or delivery.status = :status)
              and (:from is null or delivery.attemptedAt >= :from)
              and (:to is null or delivery.attemptedAt <= :to)
            """)
    long countForDashboard(
            @Param("merchantId") Long merchantId,
            @Param("status") WebhookDeliveryStatus status,
            @Param("from") java.time.Instant from,
            @Param("to") java.time.Instant to);
}
