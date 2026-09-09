package com.finpay.api.service;

import java.util.List;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finpay.api.context.AuthenticatedUser;
import com.finpay.api.context.CurrentUserProvider;
import com.finpay.api.dto.LoginRequest;
import com.finpay.api.dto.LoginResponse;
import com.finpay.api.dto.LogoutRequest;
import com.finpay.api.dto.RefreshTokenRequest;
import com.finpay.api.exception.InvalidCredentialsException;
import com.finpay.api.exception.InvalidRefreshTokenException;
import com.finpay.api.model.MerchantStatus;
import com.finpay.api.model.MerchantUser;
import com.finpay.api.model.User;
import com.finpay.api.model.UserStatus;
import com.finpay.api.repository.MerchantUserRepository;
import com.finpay.api.repository.UserRepository;
import com.finpay.api.security.IssuedRefreshToken;
import com.finpay.api.security.RefreshTokenSession;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final MerchantUserRepository merchantUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenSessionService tokenSessionService;
    private final CurrentUserProvider currentUserProvider;

    public AuthenticationService(
            UserRepository userRepository,
            MerchantUserRepository merchantUserRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            TokenSessionService tokenSessionService,
            CurrentUserProvider currentUserProvider) {
        this.userRepository = userRepository;
        this.merchantUserRepository = merchantUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenSessionService = tokenSessionService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (user.getStatus() != UserStatus.ACTIVE
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        List<MerchantUser> memberships =
                merchantUserRepository.findAllByUserIdOrderByIdAsc(user.getId());
        MerchantUser membership = memberships.stream()
                .filter(item -> item.getMerchant().getStatus() == MerchantStatus.ACTIVE)
                .findFirst()
                .orElseThrow(InvalidCredentialsException::new);

        return issueTokens(user, membership);
    }

    @Transactional(readOnly = true)
    public LoginResponse refresh(RefreshTokenRequest request) {
        RefreshTokenSession session = tokenSessionService
                .consumeRefreshToken(request.getRefreshToken())
                .orElseThrow(InvalidRefreshTokenException::new);

        MerchantUser membership = merchantUserRepository
                .findByMerchantIdAndUserId(session.merchantId(), session.userId())
                .filter(item -> item.getUser().getStatus() == UserStatus.ACTIVE)
                .filter(item -> item.getMerchant().getStatus() == MerchantStatus.ACTIVE)
                .orElseThrow(InvalidRefreshTokenException::new);

        return issueTokens(membership.getUser(), membership);
    }

    @PreAuthorize("hasAnyRole('MERCHANT_ADMIN', 'MERCHANT_USER')")
    public void logout(LogoutRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        tokenSessionService.revokeRefreshToken(
                request.getRefreshToken(),
                currentUser.userId(),
                currentUser.merchantId());
        tokenSessionService.revokeAccessToken(
                currentUser.tokenId(),
                currentUser.tokenExpiresAt());
    }

    private LoginResponse issueTokens(User user, MerchantUser membership) {
        String accessToken = jwtService.createAccessToken(
                user,
                membership.getMerchant(),
                membership.getRole());
        IssuedRefreshToken refreshToken = tokenSessionService.createRefreshToken(
                user.getId(),
                membership.getMerchant().getId());

        return new LoginResponse(
                accessToken,
                refreshToken.value(),
                "Bearer",
                jwtService.getAccessTokenTtlSeconds(),
                refreshToken.expiresIn(),
                user.getId(),
                user.getEmail(),
                membership.getMerchant().getId(),
                membership.getMerchant().getName(),
                membership.getRole());
    }
}
