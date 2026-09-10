package com.finpay.api.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.finpay.api.model.OutboxEvent;
import com.finpay.api.model.OutboxEventStatus;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findTop20ByStatusAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
            OutboxEventStatus status,
            Instant availableAt);
}
