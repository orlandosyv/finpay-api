package com.finpay.api.dto;

import java.time.Instant;

import com.finpay.api.model.MerchantRole;
import com.finpay.api.model.UserStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User membership in the authenticated merchant")
public record MerchantUserResponse(
        @Schema(description = "Membership identifier", example = "3")
        Long membershipId,
        @Schema(description = "User identifier", example = "12")
        Long userId,
        @Schema(description = "Normalized email", example = "operator@tienda.com")
        String email,
        @Schema(description = "User account status", example = "ACTIVE")
        UserStatus status,
        @Schema(description = "Role inside the merchant", example = "MERCHANT_USER")
        MerchantRole role,
        @Schema(description = "Time when the membership was created")
        Instant createdAt) {
}
