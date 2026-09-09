package com.finpay.api.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.finpay.api.model.Merchant;
import com.finpay.api.model.MerchantRole;
import com.finpay.api.model.User;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final Duration accessTokenTtl;

    public JwtService(
            JwtEncoder jwtEncoder,
            @Value("${finpay.jwt.issuer}") String issuer,
            @Value("${finpay.jwt.access-token-ttl}") Duration accessTokenTtl) {
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.accessTokenTtl = accessTokenTtl;
    }

    public String createAccessToken(User user, Merchant merchant, MerchantRole role) {
        Instant issuedAt = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .id(UUID.randomUUID().toString())
                .issuer(issuer)
                .subject(user.getId().toString())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(accessTokenTtl))
                .claim("email", user.getEmail())
                .claim("merchantId", merchant.getId())
                .claim("roles", List.of(role.name()))
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
                .type("JWT")
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();
    }

    public long getAccessTokenTtlSeconds() {
        return accessTokenTtl.toSeconds();
    }
}
