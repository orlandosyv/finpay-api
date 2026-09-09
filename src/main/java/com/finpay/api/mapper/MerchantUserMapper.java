package com.finpay.api.mapper;

import org.springframework.stereotype.Component;

import com.finpay.api.dto.CurrentMerchantResponse;
import com.finpay.api.dto.MerchantUserResponse;
import com.finpay.api.model.MerchantUser;

@Component
public class MerchantUserMapper {

    public MerchantUserResponse toResponse(MerchantUser membership) {
        return new MerchantUserResponse(
                membership.getId(),
                membership.getUser().getId(),
                membership.getUser().getEmail(),
                membership.getUser().getStatus(),
                membership.getRole(),
                membership.getCreatedAt());
    }

    public CurrentMerchantResponse toCurrentMerchantResponse(MerchantUser membership) {
        return new CurrentMerchantResponse(
                membership.getMerchant().getId(),
                membership.getMerchant().getName(),
                membership.getMerchant().getStatus(),
                membership.getUser().getId(),
                membership.getUser().getEmail(),
                membership.getRole());
    }
}
