package com.finpay.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.finpay.api.dto.CreatePaymentRequest;
import com.finpay.api.exception.GlobalExceptionHandler;
import com.finpay.api.exception.PaymentNotFoundException;
import com.finpay.api.mapper.PaymentMapper;
import com.finpay.api.model.Payment;
import com.finpay.api.model.PaymentStatus;
import com.finpay.api.service.PaymentService;

class PaymentControllerValidationTest {

    private PaymentService paymentService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        paymentService = mock(PaymentService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new PaymentController(paymentService, new PaymentMapper()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void createsPaymentWhenRequestIsValid() throws Exception {
        Payment payment = new Payment(
                new BigDecimal("250.00"),
                "PEN",
                PaymentStatus.PENDING);

        when(paymentService.createPayment(any(CreatePaymentRequest.class)))
                .thenReturn(payment);

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 250.00,
                                  "currency": "PEN"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(250.00))
                .andExpect(jsonPath("$.currency").value("PEN"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"amount\":null,\"currency\":\"PEN\"}",
            "{\"amount\":0,\"currency\":\"PEN\"}",
            "{\"amount\":-50,\"currency\":\"PEN\"}",
            "{\"amount\":10.999,\"currency\":\"PEN\"}"
    })
    void rejectsInvalidAmounts(String requestBody) throws Exception {
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.path").value("/api/payments"))
                .andExpect(jsonPath("$.fieldErrors.amount").isNotEmpty());

        verifyNoInteractions(paymentService);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"amount\":100,\"currency\":null}",
            "{\"amount\":100,\"currency\":\"\"}",
            "{\"amount\":100,\"currency\":\"pen\"}",
            "{\"amount\":100,\"currency\":\"PE\"}",
            "{\"amount\":100,\"currency\":\"XYZ\"}"
    })
    void rejectsInvalidCurrencies(String requestBody) throws Exception {
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.path").value("/api/payments"))
                .andExpect(jsonPath("$.fieldErrors.currency").isNotEmpty());

        verifyNoInteractions(paymentService);
    }

    @Test
    void keepsNonValidationErrorsWithoutFieldErrors() throws Exception {
        when(paymentService.getPaymentById(999L))
                .thenThrow(new PaymentNotFoundException(999L));

        mockMvc.perform(get("/api/payments/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Payment with id 999 was not found"))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }
}
