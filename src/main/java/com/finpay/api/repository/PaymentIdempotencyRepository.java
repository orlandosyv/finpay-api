package com.finpay.api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.finpay.api.model.PaymentIdempotency;

public interface PaymentIdempotencyRepository
        extends JpaRepository<PaymentIdempotency, Long> {

    Optional<PaymentIdempotency> findByMerchantIdAndIdempotencyKey(
            Long merchantId,
            String idempotencyKey);
}
