package com.finpay.api.config;

import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.finpay.api.security.RevokedAccessTokenValidator;

@Configuration
public class JwtConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtConfiguration.class);
    private static final int MINIMUM_SECRET_BYTES = 32;

    @Bean
    SecretKey jwtSecretKey(@Value("${finpay.jwt.secret:}") String encodedSecret) {
        if (encodedSecret == null || encodedSecret.isBlank()) {
            byte[] ephemeralSecret = new byte[MINIMUM_SECRET_BYTES];
            new SecureRandom().nextBytes(ephemeralSecret);
            LOGGER.warn(
                    "FINPAY_JWT_SECRET is not configured. Using an ephemeral development key; "
                            + "tokens will become invalid when the application restarts.");
            return new SecretKeySpec(ephemeralSecret, "HmacSHA256");
        }

        byte[] secretBytes;
        try {
            secretBytes = Base64.getDecoder().decode(encodedSecret);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("FINPAY_JWT_SECRET must be valid Base64", exception);
        }

        if (secretBytes.length < MINIMUM_SECRET_BYTES) {
            throw new IllegalStateException(
                    "FINPAY_JWT_SECRET must decode to at least 32 bytes");
        }

        return new SecretKeySpec(secretBytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return NimbusJwtEncoder.withSecretKey(jwtSecretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(
            SecretKey jwtSecretKey,
            @Value("${finpay.jwt.issuer}") String issuer,
            RevokedAccessTokenValidator revokedAccessTokenValidator) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                revokedAccessTokenValidator));
        return decoder;
    }
}
