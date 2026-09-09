package com.finpay.api.service;

import java.util.List;
import java.util.Locale;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finpay.api.context.AuthenticatedUser;
import com.finpay.api.context.CurrentUserProvider;
import com.finpay.api.dto.CreateMerchantUserRequest;
import com.finpay.api.dto.CurrentMerchantResponse;
import com.finpay.api.dto.MerchantUserResponse;
import com.finpay.api.exception.EmailAlreadyRegisteredException;
import com.finpay.api.exception.ForbiddenRequestException;
import com.finpay.api.exception.MerchantNotFoundException;
import com.finpay.api.mapper.MerchantUserMapper;
import com.finpay.api.model.Merchant;
import com.finpay.api.model.MerchantUser;
import com.finpay.api.model.User;
import com.finpay.api.model.UserStatus;
import com.finpay.api.repository.MerchantRepository;
import com.finpay.api.repository.MerchantUserRepository;
import com.finpay.api.repository.UserRepository;

@Service
public class MerchantUserService {

    private final MerchantRepository merchantRepository;
    private final UserRepository userRepository;
    private final MerchantUserRepository merchantUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserProvider currentUserProvider;
    private final MerchantUserMapper merchantUserMapper;

    public MerchantUserService(
            MerchantRepository merchantRepository,
            UserRepository userRepository,
            MerchantUserRepository merchantUserRepository,
            PasswordEncoder passwordEncoder,
            CurrentUserProvider currentUserProvider,
            MerchantUserMapper merchantUserMapper) {
        this.merchantRepository = merchantRepository;
        this.userRepository = userRepository;
        this.merchantUserRepository = merchantUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUserProvider = currentUserProvider;
        this.merchantUserMapper = merchantUserMapper;
    }

    @PreAuthorize("hasAnyRole('MERCHANT_ADMIN', 'MERCHANT_USER')")
    @Transactional(readOnly = true)
    public CurrentMerchantResponse getCurrentMerchant() {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        MerchantUser membership = merchantUserRepository
                .findByMerchantIdAndUserId(currentUser.merchantId(), currentUser.userId())
                .orElseThrow(ForbiddenRequestException::new);
        return merchantUserMapper.toCurrentMerchantResponse(membership);
    }

    @PreAuthorize("hasRole('MERCHANT_ADMIN')")
    @Transactional(readOnly = true)
    public List<MerchantUserResponse> getMerchantUsers() {
        Long merchantId = currentUserProvider.getCurrentUser().merchantId();
        return merchantUserRepository.findAllByMerchantIdOrderByIdAsc(merchantId)
                .stream()
                .map(merchantUserMapper::toResponse)
                .toList();
    }

    @PreAuthorize("hasRole('MERCHANT_ADMIN')")
    @Transactional
    public MerchantUserResponse createMerchantUser(CreateMerchantUserRequest request) {
        Long merchantId = currentUserProvider.getCurrentUser().merchantId();
        String normalizedEmail = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException(normalizedEmail);
        }

        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException(merchantId));
        User user = userRepository.save(new User(
                normalizedEmail,
                passwordEncoder.encode(request.getPassword()),
                UserStatus.ACTIVE));
        MerchantUser membership = merchantUserRepository.save(new MerchantUser(
                merchant,
                user,
                request.getRole()));

        return merchantUserMapper.toResponse(membership);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
