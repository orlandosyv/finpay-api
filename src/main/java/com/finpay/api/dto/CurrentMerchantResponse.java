package com.finpay.api.dto;

import com.finpay.api.model.MerchantRole;
import com.finpay.api.model.MerchantStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Authenticated user and merchant context")
public record CurrentMerchantResponse(
        @Schema(description = "Merchant identifier", example = "2")
        Long merchantId,
        @Schema(description = "Merchant display name", example = "Tienda Andina")
        String merchantName,
        @Schema(description = "Merchant status", example = "ACTIVE")
        MerchantStatus merchantStatus,
        @Schema(description = "Authenticated user identifier", example = "10")
        Long userId,
        @Schema(description = "Authenticated user email", example = "admin@tienda.com")
        String email,
        @Schema(description = "Role inside the merchant", example = "MERCHANT_ADMIN")
        MerchantRole role) {
}
