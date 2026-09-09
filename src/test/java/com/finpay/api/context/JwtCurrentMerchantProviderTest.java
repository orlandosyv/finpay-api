package com.finpay.api.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class JwtCurrentMerchantProviderTest {

    private final JwtCurrentMerchantProvider provider = new JwtCurrentMerchantProvider();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void obtainsMerchantIdFromAuthenticatedJwt() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("10")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("merchantId", 42L)
                .claim("roles", List.of("MERCHANT_ADMIN"))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_MERCHANT_ADMIN"))));

        assertThat(provider.getCurrentMerchantId()).isEqualTo(42L);
    }

    @Test
    void rejectsCallsWithoutAuthenticatedJwt() {
        assertThatThrownBy(provider::getCurrentMerchantId)
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("An authenticated merchant is required");
    }
}
