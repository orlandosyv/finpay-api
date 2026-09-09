package com.finpay.api.dto;

import com.finpay.api.model.MerchantRole;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Registered merchant and administrator")
public record RegistrationResponse(
        @Schema(description = "Merchant identifier", example = "2")
        Long merchantId,
        @Schema(description = "Merchant display name", example = "Tienda Andina")
        String merchantName,
        @Schema(description = "Administrator user identifier", example = "1")
        Long userId,
        @Schema(description = "Normalized administrator email", example = "admin@tienda.com")
        String email,
        @Schema(description = "Initial merchant role", example = "MERCHANT_ADMIN")
        MerchantRole role) {
}
