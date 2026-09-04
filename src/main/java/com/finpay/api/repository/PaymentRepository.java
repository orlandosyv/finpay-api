package com.finpay.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.finpay.api.model.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
