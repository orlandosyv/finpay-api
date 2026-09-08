package com.finpay.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.finpay.api.model.Merchant;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {
}
