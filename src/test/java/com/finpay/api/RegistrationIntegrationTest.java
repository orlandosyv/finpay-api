package com.finpay.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.finpay.api.config.TestcontainersConfiguration;
import com.finpay.api.dto.RegisterMerchantRequest;
import com.finpay.api.dto.RegistrationResponse;
import com.finpay.api.exception.EmailAlreadyRegisteredException;
import com.finpay.api.model.MerchantRole;
import com.finpay.api.model.MerchantUser;
import com.finpay.api.model.User;
import com.finpay.api.model.UserStatus;
import com.finpay.api.repository.MerchantRepository;
import com.finpay.api.repository.MerchantUserRepository;
import com.finpay.api.repository.UserRepository;
import com.finpay.api.service.RegistrationService;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class RegistrationIntegrationTest {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MerchantUserRepository merchantUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registersMerchantUserAndAdminMembership() {
        long merchantsBefore = merchantRepository.count();
        long usersBefore = userRepository.count();
        long membershipsBefore = merchantUserRepository.count();
        String rawPassword = "StrongPassword123!";

        RegistrationResponse response = registrationService.register(registrationRequest(
                "  Tienda Andina  ",
                "  ADMIN@TIENDA.COM  ",
                rawPassword));

        merchantUserRepository.flush();

        User storedUser = userRepository.findByEmailIgnoreCase("admin@tienda.com")
                .orElseThrow();
        List<MerchantUser> memberships = merchantUserRepository.findAllByUserId(storedUser.getId());

        assertThat(merchantRepository.count()).isEqualTo(merchantsBefore + 1);
        assertThat(userRepository.count()).isEqualTo(usersBefore + 1);
        assertThat(merchantUserRepository.count()).isEqualTo(membershipsBefore + 1);

        assertThat(storedUser.getEmail()).isEqualTo("admin@tienda.com");
        assertThat(storedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(storedUser.getPasswordHash()).isNotEqualTo(rawPassword);
        assertThat(storedUser.getPasswordHash()).startsWith("{bcrypt}");
        assertThat(passwordEncoder.matches(rawPassword, storedUser.getPasswordHash())).isTrue();
        assertThat(storedUser.getCreatedAt()).isNotNull();
        assertThat(storedUser.getUpdatedAt()).isNotNull();

        assertThat(memberships).singleElement().satisfies(membership -> {
            assertThat(membership.getMerchant().getId()).isEqualTo(response.merchantId());
            assertThat(membership.getMerchant().getName()).isEqualTo("Tienda Andina");
            assertThat(membership.getUser().getId()).isEqualTo(response.userId());
            assertThat(membership.getRole()).isEqualTo(MerchantRole.MERCHANT_ADMIN);
            assertThat(membership.getCreatedAt()).isNotNull();
            assertThat(membership.getUpdatedAt()).isNotNull();
        });

        assertThat(response.merchantName()).isEqualTo("Tienda Andina");
        assertThat(response.email()).isEqualTo("admin@tienda.com");
        assertThat(response.role()).isEqualTo(MerchantRole.MERCHANT_ADMIN);
    }

    @Test
    void rejectsDuplicateEmailWithoutCreatingMoreData() {
        RegisterMerchantRequest firstRequest = registrationRequest(
                "First Shop",
                "owner@shop.com",
                "StrongPassword123!");
        registrationService.register(firstRequest);

        long merchantCount = merchantRepository.count();
        long userCount = userRepository.count();
        long membershipCount = merchantUserRepository.count();

        RegisterMerchantRequest duplicateRequest = registrationRequest(
                "Second Shop",
                "OWNER@SHOP.COM",
                "AnotherPassword456!");

        assertThatThrownBy(() -> registrationService.register(duplicateRequest))
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessage("Email owner@shop.com is already registered");

        assertThat(merchantRepository.count()).isEqualTo(merchantCount);
        assertThat(userRepository.count()).isEqualTo(userCount);
        assertThat(merchantUserRepository.count()).isEqualTo(membershipCount);
    }

    private RegisterMerchantRequest registrationRequest(
            String merchantName,
            String email,
            String password) {
        RegisterMerchantRequest request = new RegisterMerchantRequest();
        request.setMerchantName(merchantName);
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }
}
