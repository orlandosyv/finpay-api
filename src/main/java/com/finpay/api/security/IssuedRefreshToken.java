package com.finpay.api.security;

public record IssuedRefreshToken(String value, long expiresIn) {
}
