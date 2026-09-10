package com.finpay.api.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.finpay.api.model.OutboxEvent;
import com.finpay.api.model.OutboxEventStatus;

public interface OutboxEventRepository extends
        JpaRepository<OutboxEvent, UUID>,
        JpaSpecificationExecutor<OutboxEvent> {

    List<OutboxEvent> findTop20ByStatusAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
            OutboxEventStatus status,
            Instant availableAt);

    @Query("""
            select event
            from OutboxEvent event
            where event.id = :eventId
              and event.merchant.id = :merchantId
            """)
    java.util.Optional<OutboxEvent> findByIdAndMerchantId(
            @Param("eventId") UUID eventId,
            @Param("merchantId") Long merchantId);
}
