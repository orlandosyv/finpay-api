package com.finpay.api.context;

import java.util.Set;

import com.finpay.api.model.MerchantRole;

public record AuthenticatedUser(
        Long userId,
        String email,
        Long merchantId,
        Set<MerchantRole> roles) {
}
