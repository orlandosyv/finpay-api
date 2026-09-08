package com.finpay.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.finpay.api.model.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findAllByMerchantId(Long merchantId);

    Optional<Payment> findByIdAndMerchantId(Long id, Long merchantId);
}
