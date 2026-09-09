package com.finpay.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.finpay.api.model.Merchant;
import com.finpay.api.model.MerchantRole;
import com.finpay.api.model.MerchantStatus;
import com.finpay.api.model.User;
import com.finpay.api.model.UserStatus;

class JwtServiceTest {

    @Test
    void createsSignedTokenWithIdentityTenantRoleAndExpiration() {
        SecretKey secretKey = new SecretKeySpec(
                "0123456789abcdef0123456789abcdef".getBytes(),
                "HmacSHA256");
        NimbusJwtEncoder encoder = NimbusJwtEncoder.withSecretKey(secretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        JwtService jwtService = new JwtService(encoder, "https://finpay.local", Duration.ofMinutes(15));
        User user = new User("admin@tienda.com", "hash", UserStatus.ACTIVE);
        Merchant merchant = new Merchant("Tienda Andina", MerchantStatus.ACTIVE);
        ReflectionTestUtils.setField(user, "id", 10L);
        ReflectionTestUtils.setField(merchant, "id", 2L);

        Instant beforeCreation = Instant.now();
        String tokenValue = jwtService.createAccessToken(
                user,
                merchant,
                MerchantRole.MERCHANT_ADMIN);
        Jwt token = decoder.decode(tokenValue);

        assertThat(token.getSubject()).isEqualTo("10");
        assertThat(token.getIssuer().toString()).isEqualTo("https://finpay.local");
        assertThat(token.getClaimAsString("email")).isEqualTo("admin@tienda.com");
        assertThat(token.<Number>getClaim("merchantId").longValue()).isEqualTo(2L);
        assertThat(token.getClaimAsStringList("roles"))
                .containsExactly("MERCHANT_ADMIN");
        assertThat(token.getIssuedAt()).isAfterOrEqualTo(
                beforeCreation.truncatedTo(ChronoUnit.SECONDS));
        assertThat(token.getExpiresAt()).isAfter(token.getIssuedAt());
        assertThat(jwtService.getAccessTokenTtlSeconds()).isEqualTo(900);
    }
}
