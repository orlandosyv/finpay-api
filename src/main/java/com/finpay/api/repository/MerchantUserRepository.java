package com.finpay.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.finpay.api.model.MerchantUser;

public interface MerchantUserRepository extends JpaRepository<MerchantUser, Long> {

    @EntityGraph(attributePaths = {"merchant", "user"})
    List<MerchantUser> findAllByUserIdOrderByIdAsc(Long userId);

    @EntityGraph(attributePaths = {"merchant", "user"})
    List<MerchantUser> findAllByMerchantIdOrderByIdAsc(Long merchantId);

    @EntityGraph(attributePaths = {"merchant", "user"})
    Optional<MerchantUser> findByMerchantIdAndUserId(Long merchantId, Long userId);
}
