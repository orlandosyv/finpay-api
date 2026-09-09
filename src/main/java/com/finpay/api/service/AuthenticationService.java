package com.finpay.api.service;

import java.util.List;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finpay.api.dto.LoginRequest;
import com.finpay.api.dto.LoginResponse;
import com.finpay.api.exception.InvalidCredentialsException;
import com.finpay.api.model.MerchantStatus;
import com.finpay.api.model.MerchantUser;
import com.finpay.api.model.User;
import com.finpay.api.model.UserStatus;
import com.finpay.api.repository.MerchantUserRepository;
import com.finpay.api.repository.UserRepository;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final MerchantUserRepository merchantUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthenticationService(
            UserRepository userRepository,
            MerchantUserRepository merchantUserRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.merchantUserRepository = merchantUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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

        String accessToken = jwtService.createAccessToken(
                user,
                membership.getMerchant(),
                membership.getRole());

        return new LoginResponse(
                accessToken,
                "Bearer",
                jwtService.getAccessTokenTtlSeconds(),
                user.getId(),
                user.getEmail(),
                membership.getMerchant().getId(),
                membership.getMerchant().getName(),
                membership.getRole());
    }
}
