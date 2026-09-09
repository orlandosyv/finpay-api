package com.finpay.api.context;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.finpay.api.model.MerchantRole;

@Component
public class JwtCurrentUserProvider implements CurrentUserProvider {

    @Override
    public AuthenticatedUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)
                || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("An authenticated user is required");
        }

        try {
            Long userId = Long.valueOf(jwtAuthentication.getToken().getSubject());
            Object merchantIdClaim = jwtAuthentication.getToken().getClaim("merchantId");
            String email = jwtAuthentication.getToken().getClaimAsString("email");
            Object rolesClaim = jwtAuthentication.getToken().getClaim("roles");

            if (!(merchantIdClaim instanceof Number merchantId)
                    || email == null
                    || email.isBlank()
                    || !(rolesClaim instanceof Collection<?> rawRoles)) {
                throw new AccessDeniedException("The access token does not contain a valid user context");
            }

            Set<MerchantRole> roles = rawRoles.stream()
                    .map(String::valueOf)
                    .map(MerchantRole::valueOf)
                    .collect(Collectors.toUnmodifiableSet());
            if (roles.isEmpty()) {
                throw new AccessDeniedException("The access token does not contain a valid user context");
            }

            return new AuthenticatedUser(
                    userId,
                    email,
                    merchantId.longValue(),
                    roles);
        } catch (IllegalArgumentException exception) {
            throw new AccessDeniedException(
                    "The access token does not contain a valid user context",
                    exception);
        }
    }
}
