package com.finpay.api.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.finpay.api.service.TokenSessionService;

@Component
public class RevokedAccessTokenValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error REVOKED_TOKEN_ERROR = new OAuth2Error(
            "invalid_token",
            "The access token has been revoked",
            null);

    private final TokenSessionService tokenSessionService;

    public RevokedAccessTokenValidator(TokenSessionService tokenSessionService) {
        this.tokenSessionService = tokenSessionService;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        String tokenId = token.getId();
        if (tokenId == null || tokenSessionService.isAccessTokenRevoked(tokenId)) {
            return OAuth2TokenValidatorResult.failure(REVOKED_TOKEN_ERROR);
        }
        return OAuth2TokenValidatorResult.success();
    }
}
