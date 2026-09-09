package com.finpay.api.service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.finpay.api.security.IssuedRefreshToken;
import com.finpay.api.security.RefreshTokenSession;

@Service
public class TokenSessionService {

    private static final String REFRESH_KEY_PREFIX = "finpay:auth:refresh:";
    private static final String REVOKED_ACCESS_KEY_PREFIX = "finpay:auth:revoked-access:";
    private static final int REFRESH_TOKEN_BYTES = 32;

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Duration refreshTokenTtl;

    public TokenSessionService(
            StringRedisTemplate redisTemplate,
            @Value("${finpay.jwt.refresh-token-ttl}") Duration refreshTokenTtl) {
        this.redisTemplate = redisTemplate;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    public IssuedRefreshToken createRefreshToken(Long userId, Long merchantId) {
        byte[] tokenBytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        redisTemplate.opsForValue().set(
                refreshKey(token),
                serialize(new RefreshTokenSession(userId, merchantId)),
                refreshTokenTtl);

        return new IssuedRefreshToken(token, refreshTokenTtl.toSeconds());
    }

    public Optional<RefreshTokenSession> consumeRefreshToken(String token) {
        String serializedSession = redisTemplate.opsForValue().getAndDelete(refreshKey(token));
        return Optional.ofNullable(serializedSession).map(this::deserialize);
    }

    public void revokeRefreshToken(String token, Long userId, Long merchantId) {
        String key = refreshKey(token);
        String serializedSession = redisTemplate.opsForValue().get(key);
        if (serializedSession == null) {
            return;
        }

        RefreshTokenSession session = deserialize(serializedSession);
        if (session.userId().equals(userId) && session.merchantId().equals(merchantId)) {
            redisTemplate.delete(key);
        }
    }

    public void revokeAccessToken(String tokenId, Instant expiresAt) {
        Duration remainingTtl = Duration.between(Instant.now(), expiresAt);
        if (remainingTtl.isNegative() || remainingTtl.isZero()) {
            return;
        }

        redisTemplate.opsForValue().set(
                REVOKED_ACCESS_KEY_PREFIX + tokenId,
                "revoked",
                remainingTtl);
    }

    public boolean isAccessTokenRevoked(String tokenId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(REVOKED_ACCESS_KEY_PREFIX + tokenId));
    }

    private String refreshKey(String token) {
        try {
            byte[] tokenHash = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return REFRESH_KEY_PREFIX + HexFormat.of().formatHex(tokenHash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String serialize(RefreshTokenSession session) {
        return session.userId() + ":" + session.merchantId();
    }

    private RefreshTokenSession deserialize(String value) {
        String[] parts = value.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalStateException("Invalid refresh token session stored in Redis");
        }
        return new RefreshTokenSession(Long.valueOf(parts[0]), Long.valueOf(parts[1]));
    }
}
