package com.finpay.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.finpay.api.context.CurrentUserProvider;
import com.finpay.api.dto.LoginRequest;
import com.finpay.api.dto.LoginResponse;
import com.finpay.api.exception.InvalidCredentialsException;
import com.finpay.api.model.Merchant;
import com.finpay.api.model.MerchantRole;
import com.finpay.api.model.MerchantStatus;
import com.finpay.api.model.MerchantUser;
import com.finpay.api.model.User;
import com.finpay.api.model.UserStatus;
import com.finpay.api.repository.MerchantUserRepository;
import com.finpay.api.repository.UserRepository;
import com.finpay.api.security.IssuedRefreshToken;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MerchantUserRepository merchantUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private TokenSessionService tokenSessionService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void authenticatesActiveUserAndReturnsMerchantToken() {
        User user = activeUser();
        Merchant merchant = activeMerchant();
        MerchantUser membership = new MerchantUser(
                merchant,
                user,
                MerchantRole.MERCHANT_ADMIN);
        LoginRequest request = loginRequest("  ADMIN@TIENDA.COM  ", "StrongPassword123!");

        when(userRepository.findByEmailIgnoreCase("admin@tienda.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("StrongPassword123!", "{bcrypt}stored-hash"))
                .thenReturn(true);
        when(merchantUserRepository.findAllByUserIdOrderByIdAsc(10L))
                .thenReturn(List.of(membership));
        when(jwtService.createAccessToken(user, merchant, MerchantRole.MERCHANT_ADMIN))
                .thenReturn("signed.jwt.token");
        when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);
        when(tokenSessionService.createRefreshToken(10L, 2L))
                .thenReturn(new IssuedRefreshToken("opaque-refresh-token", 604800L));

        LoginResponse response = authenticationService.login(request);

        assertThat(response.accessToken()).isEqualTo("signed.jwt.token");
        assertThat(response.refreshToken()).isEqualTo("opaque-refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900);
        assertThat(response.refreshExpiresIn()).isEqualTo(604800);
        assertThat(response.userId()).isEqualTo(10L);
        assertThat(response.email()).isEqualTo("admin@tienda.com");
        assertThat(response.merchantId()).isEqualTo(2L);
        assertThat(response.merchantName()).isEqualTo("Tienda Andina");
        assertThat(response.role()).isEqualTo(MerchantRole.MERCHANT_ADMIN);
    }

    @Test
    void rejectsUnknownEmailWithoutRevealingWhetherItExists() {
        LoginRequest request = loginRequest("unknown@tienda.com", "StrongPassword123!");
        when(userRepository.findByEmailIgnoreCase("unknown@tienda.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");

        verify(passwordEncoder, never()).matches(
                "StrongPassword123!",
                "{bcrypt}stored-hash");
    }

    @Test
    void rejectsIncorrectPassword() {
        User user = activeUser();
        LoginRequest request = loginRequest("admin@tienda.com", "WrongPassword123!");
        when(userRepository.findByEmailIgnoreCase("admin@tienda.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword123!", "{bcrypt}stored-hash"))
                .thenReturn(false);

        assertThatThrownBy(() -> authenticationService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");

        verify(merchantUserRepository, never()).findAllByUserIdOrderByIdAsc(10L);
    }

    private User activeUser() {
        User user = new User("admin@tienda.com", "{bcrypt}stored-hash", UserStatus.ACTIVE);
        ReflectionTestUtils.setField(user, "id", 10L);
        return user;
    }

    private Merchant activeMerchant() {
        Merchant merchant = new Merchant("Tienda Andina", MerchantStatus.ACTIVE);
        ReflectionTestUtils.setField(merchant, "id", 2L);
        return merchant;
    }

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }
}
