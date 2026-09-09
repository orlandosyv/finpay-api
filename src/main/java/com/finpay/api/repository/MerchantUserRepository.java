package com.finpay.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.finpay.api.model.MerchantUser;

public interface MerchantUserRepository extends JpaRepository<MerchantUser, Long> {

    List<MerchantUser> findAllByUserId(Long userId);
}
