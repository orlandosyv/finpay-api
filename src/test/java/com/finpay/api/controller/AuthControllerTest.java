package com.finpay.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.finpay.api.dto.RegisterMerchantRequest;
import com.finpay.api.dto.RegistrationResponse;
import com.finpay.api.exception.EmailAlreadyRegisteredException;
import com.finpay.api.exception.GlobalExceptionHandler;
import com.finpay.api.model.MerchantRole;
import com.finpay.api.service.RegistrationService;

class AuthControllerTest {

    private RegistrationService registrationService;
    private LocalValidatorFactoryBean validator;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        registrationService = mock(RegistrationService.class);
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(registrationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    @Test
    void registersMerchantAndReturnsCreatedResponse() throws Exception {
        RegistrationResponse response = new RegistrationResponse(
                2L,
                "Tienda Andina",
                10L,
                "admin@tienda.com",
                MerchantRole.MERCHANT_ADMIN);
        when(registrationService.register(any(RegisterMerchantRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "merchantName": "Tienda Andina",
                                  "email": "admin@tienda.com",
                                  "password": "StrongPassword123!"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.merchantId").value(2))
                .andExpect(jsonPath("$.merchantName").value("Tienda Andina"))
                .andExpect(jsonPath("$.userId").value(10))
                .andExpect(jsonPath("$.email").value("admin@tienda.com"))
                .andExpect(jsonPath("$.role").value("MERCHANT_ADMIN"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        verify(registrationService).register(any(RegisterMerchantRequest.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"merchantName\":\"\",\"email\":\"admin@tienda.com\",\"password\":\"StrongPassword123!\"}",
            "{\"merchantName\":\"Tienda\",\"email\":\"invalid\",\"password\":\"StrongPassword123!\"}",
            "{\"merchantName\":\"Tienda\",\"email\":\"admin@tienda.com\",\"password\":\"short\"}",
            "{\"merchantName\":\"Tienda\",\"email\":\"admin@tienda.com\",\"password\":\"onlylowercase123!\"}",
            "{\"merchantName\":\"Tienda\",\"email\":\"admin@tienda.com\",\"password\":\"NoSpecialPassword123\"}"
    })
    void rejectsInvalidRegistrationData(String requestBody) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors").isNotEmpty());

        verifyNoInteractions(registrationService);
    }

    @Test
    void returnsConflictWhenEmailIsAlreadyRegistered() throws Exception {
        when(registrationService.register(any(RegisterMerchantRequest.class)))
                .thenThrow(new EmailAlreadyRegisteredException("admin@tienda.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "merchantName": "Tienda Andina",
                                  "email": "admin@tienda.com",
                                  "password": "StrongPassword123!"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("Email admin@tienda.com is already registered"))
                .andExpect(jsonPath("$.path").value("/api/auth/register"));
    }
}
