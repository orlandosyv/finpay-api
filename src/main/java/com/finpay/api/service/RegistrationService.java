package com.finpay.api.service;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finpay.api.dto.RegisterMerchantRequest;
import com.finpay.api.dto.RegistrationResponse;
import com.finpay.api.exception.EmailAlreadyRegisteredException;
import com.finpay.api.model.Merchant;
import com.finpay.api.model.MerchantRole;
import com.finpay.api.model.MerchantStatus;
import com.finpay.api.model.MerchantUser;
import com.finpay.api.model.User;
import com.finpay.api.model.UserStatus;
import com.finpay.api.repository.MerchantRepository;
import com.finpay.api.repository.MerchantUserRepository;
import com.finpay.api.repository.UserRepository;

@Service
public class RegistrationService {

    private final MerchantRepository merchantRepository;
    private final UserRepository userRepository;
    private final MerchantUserRepository merchantUserRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(
            MerchantRepository merchantRepository,
            UserRepository userRepository,
            MerchantUserRepository merchantUserRepository,
            PasswordEncoder passwordEncoder) {
        this.merchantRepository = merchantRepository;
        this.userRepository = userRepository;
        this.merchantUserRepository = merchantUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegistrationResponse register(RegisterMerchantRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException(normalizedEmail);
        }

        Merchant merchant = merchantRepository.save(new Merchant(
                request.getMerchantName().trim(),
                MerchantStatus.ACTIVE));

        User user = userRepository.save(new User(
                normalizedEmail,
                passwordEncoder.encode(request.getPassword()),
                UserStatus.ACTIVE));

        MerchantUser membership = merchantUserRepository.save(new MerchantUser(
                merchant,
                user,
                MerchantRole.MERCHANT_ADMIN));

        return new RegistrationResponse(
                merchant.getId(),
                merchant.getName(),
                user.getId(),
                user.getEmail(),
                membership.getRole());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
