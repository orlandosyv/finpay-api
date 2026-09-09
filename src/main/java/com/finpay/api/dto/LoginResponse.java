package com.finpay.api.dto;

import com.finpay.api.model.MerchantRole;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JWT access token and authenticated merchant context")
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        Long userId,
        String email,
        Long merchantId,
        String merchantName,
        MerchantRole role) {
}
