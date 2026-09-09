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

import com.finpay.api.model.MerchantRole;

class JwtCurrentUserProviderTest {

    private final JwtCurrentUserProvider provider = new JwtCurrentUserProvider();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void obtainsAuthenticatedUserFromJwtClaims() {
        authenticate("10", 42L, "operator@tienda.com", List.of("MERCHANT_USER"));

        AuthenticatedUser currentUser = provider.getCurrentUser();

        assertThat(currentUser.userId()).isEqualTo(10L);
        assertThat(currentUser.merchantId()).isEqualTo(42L);
        assertThat(currentUser.email()).isEqualTo("operator@tienda.com");
        assertThat(currentUser.roles()).containsExactly(MerchantRole.MERCHANT_USER);
    }

    @Test
    void rejectsJwtWithoutRequiredUserClaims() {
        authenticate("10", 42L, null, List.of("MERCHANT_USER"));

        assertThatThrownBy(provider::getCurrentUser)
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("The access token does not contain a valid user context");
    }

    @Test
    void rejectsCallsWithoutAuthenticatedJwt() {
        assertThatThrownBy(provider::getCurrentUser)
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("An authenticated user is required");
    }

    private void authenticate(
            String subject,
            Long merchantId,
            String email,
            List<String> roles) {
        Jwt.Builder jwtBuilder = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("merchantId", merchantId)
                .claim("jti", "test-token-id")
                .claim("roles", roles);
        if (email != null) {
            jwtBuilder.claim("email", email);
        }

        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                jwtBuilder.build(),
                List.of(new SimpleGrantedAuthority("ROLE_" + roles.getFirst()))));
    }
}
