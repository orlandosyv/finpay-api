package com.finpay.api.context;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class JwtCurrentMerchantProvider implements CurrentMerchantProvider {

    @Override
    public Long getCurrentMerchantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)
                || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("An authenticated merchant is required");
        }

        Object merchantIdClaim = jwtAuthentication.getToken().getClaim("merchantId");
        if (merchantIdClaim instanceof Number merchantId) {
            return merchantId.longValue();
        }

        throw new AccessDeniedException("The access token does not contain a valid merchantId");
    }
}
