package com.finpay.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.finpay.api.dto.RegisterMerchantRequest;
import com.finpay.api.dto.RegistrationResponse;
import com.finpay.api.exception.EmailAlreadyRegisteredException;
import com.finpay.api.model.Merchant;
import com.finpay.api.model.MerchantRole;
import com.finpay.api.model.MerchantUser;
import com.finpay.api.model.User;
import com.finpay.api.model.UserStatus;
import com.finpay.api.repository.MerchantRepository;
import com.finpay.api.repository.MerchantUserRepository;
import com.finpay.api.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MerchantUserRepository merchantUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private RegistrationService registrationService;

    @Test
    void registersMerchantWithNormalizedEmailAndEncodedPassword() {
        RegisterMerchantRequest request = registrationRequest(
                "  Tienda Andina  ",
                "  ADMIN@TIENDA.COM  ",
                "StrongPassword123!");

        when(userRepository.existsByEmailIgnoreCase("admin@tienda.com")).thenReturn(false);
        when(passwordEncoder.encode("StrongPassword123!")).thenReturn("{bcrypt}encoded-password");
        when(merchantRepository.save(any(Merchant.class))).thenAnswer(invocation -> {
            Merchant merchant = invocation.getArgument(0);
            ReflectionTestUtils.setField(merchant, "id", 2L);
            return merchant;
        });
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 10L);
            return user;
        });
        when(merchantUserRepository.save(any(MerchantUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RegistrationResponse response = registrationService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<MerchantUser> membershipCaptor = ArgumentCaptor.forClass(MerchantUser.class);
        verify(userRepository).save(userCaptor.capture());
        verify(merchantUserRepository).save(membershipCaptor.capture());

        User savedUser = userCaptor.getValue();
        MerchantUser savedMembership = membershipCaptor.getValue();

        assertThat(savedUser.getEmail()).isEqualTo("admin@tienda.com");
        assertThat(savedUser.getPasswordHash()).isEqualTo("{bcrypt}encoded-password");
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(savedMembership.getMerchant().getName()).isEqualTo("Tienda Andina");
        assertThat(savedMembership.getUser()).isSameAs(savedUser);
        assertThat(savedMembership.getRole()).isEqualTo(MerchantRole.MERCHANT_ADMIN);

        assertThat(response.merchantId()).isEqualTo(2L);
        assertThat(response.merchantName()).isEqualTo("Tienda Andina");
        assertThat(response.userId()).isEqualTo(10L);
        assertThat(response.email()).isEqualTo("admin@tienda.com");
        assertThat(response.role()).isEqualTo(MerchantRole.MERCHANT_ADMIN);
    }

    @Test
    void rejectsAlreadyRegisteredEmailBeforeCreatingData() {
        RegisterMerchantRequest request = registrationRequest(
                "Tienda Andina",
                "ADMIN@TIENDA.COM",
                "StrongPassword123!");
        when(userRepository.existsByEmailIgnoreCase("admin@tienda.com")).thenReturn(true);

        assertThatThrownBy(() -> registrationService.register(request))
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessage("Email admin@tienda.com is already registered");

        verify(merchantRepository, never()).save(any(Merchant.class));
        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(merchantUserRepository, passwordEncoder);
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
