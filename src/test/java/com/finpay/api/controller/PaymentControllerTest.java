package com.finpay.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.finpay.api.dto.CreatePaymentRequest;
import com.finpay.api.dto.IdempotentPaymentResult;
import com.finpay.api.exception.GlobalExceptionHandler;
import com.finpay.api.exception.InvalidPaymentStatusTransitionException;
import com.finpay.api.exception.PaymentNotFoundException;
import com.finpay.api.mapper.PaymentMapper;
import com.finpay.api.model.Merchant;
import com.finpay.api.model.MerchantStatus;
import com.finpay.api.model.Payment;
import com.finpay.api.model.PaymentStatus;
import com.finpay.api.service.PaymentService;
import com.finpay.api.service.PaymentIdempotencyService;

class PaymentControllerTest {

    private static final Instant CREATED_AT = Instant.parse("2026-09-07T20:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-09-07T20:05:00Z");

    private PaymentService paymentService;
    private PaymentIdempotencyService paymentIdempotencyService;
    private LocalValidatorFactoryBean validator;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        paymentService = mock(PaymentService.class);
        paymentIdempotencyService = mock(PaymentIdempotencyService.class);
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new PaymentController(
                        paymentService,
                        new PaymentMapper(),
                        paymentIdempotencyService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    @Test
    void returnsAllPaymentsAsPaymentResponses() throws Exception {
        Payment pendingPayment = payment(1L, PaymentStatus.PENDING);
        Payment approvedPayment = payment(2L, PaymentStatus.APPROVED);
        when(paymentService.getAllPayments())
                .thenReturn(List.of(pendingPayment, approvedPayment));

        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].amount").value(100.00))
                .andExpect(jsonPath("$[0].currency").value("PEN"))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].createdAt").value(CREATED_AT.toString()))
                .andExpect(jsonPath("$[0].updatedAt").value(UPDATED_AT.toString()))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].status").value("APPROVED"));

        verify(paymentService).getAllPayments();
    }

    @Test
    void returnsPaymentByIdAsPaymentResponse() throws Exception {
        Payment payment = payment(1L, PaymentStatus.PENDING);
        when(paymentService.getPaymentById(1L)).thenReturn(payment);

        mockMvc.perform(get("/api/payments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.currency").value("PEN"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.createdAt").value(CREATED_AT.toString()))
                .andExpect(jsonPath("$.updatedAt").value(UPDATED_AT.toString()));

        verify(paymentService).getPaymentById(1L);
    }

    @Test
    void createsPaymentAndReturnsCreatedStatus() throws Exception {
        Payment payment = payment(1L, PaymentStatus.PENDING);
        when(paymentIdempotencyService.createPayment(
                org.mockito.ArgumentMatchers.eq("payment-test-1"),
                any(CreatePaymentRequest.class)))
                .thenReturn(new IdempotentPaymentResult(
                        new PaymentMapper().toResponse(payment),
                        false));

        mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", "payment-test-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 100.00,
                                  "currency": "PEN"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.createdAt").value(CREATED_AT.toString()))
                .andExpect(jsonPath("$.updatedAt").value(UPDATED_AT.toString()));

        verify(paymentIdempotencyService).createPayment(
                org.mockito.ArgumentMatchers.eq("payment-test-1"),
                any(CreatePaymentRequest.class));
    }

    @Test
    void approvesPayment() throws Exception {
        Payment payment = payment(1L, PaymentStatus.APPROVED);
        when(paymentService.approvePayment(1L)).thenReturn(payment);

        mockMvc.perform(patch("/api/payments/1/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("APPROVED"));

        verify(paymentService).approvePayment(1L);
    }

    @Test
    void declinesPayment() throws Exception {
        Payment payment = payment(1L, PaymentStatus.DECLINED);
        when(paymentService.declinePayment(1L)).thenReturn(payment);

        mockMvc.perform(patch("/api/payments/1/decline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("DECLINED"));

        verify(paymentService).declinePayment(1L);
    }

    @Test
    void refundsPayment() throws Exception {
        Payment payment = payment(1L, PaymentStatus.REFUNDED);
        when(paymentService.refundPayment(1L)).thenReturn(payment);

        mockMvc.perform(patch("/api/payments/1/refund"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("REFUNDED"));

        verify(paymentService).refundPayment(1L);
    }

    @Test
    void returnsNotFoundWhenPaymentDoesNotExist() throws Exception {
        when(paymentService.getPaymentById(999L))
                .thenThrow(new PaymentNotFoundException(999L));

        mockMvc.perform(get("/api/payments/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Payment with id 999 was not found"))
                .andExpect(jsonPath("$.path").value("/api/payments/999"));

        verify(paymentService).getPaymentById(999L);
    }

    @Test
    void returnsConflictWhenTransitionIsInvalid() throws Exception {
        when(paymentService.refundPayment(1L))
                .thenThrow(new InvalidPaymentStatusTransitionException(
                        PaymentStatus.DECLINED,
                        PaymentStatus.REFUNDED));

        mockMvc.perform(patch("/api/payments/1/refund"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("Payment cannot transition from DECLINED to REFUNDED"))
                .andExpect(jsonPath("$.path").value("/api/payments/1/refund"));

        verify(paymentService).refundPayment(1L);
    }

    private Payment payment(Long id, PaymentStatus status) {
        Payment payment = new Payment(
                new Merchant("Test Merchant", MerchantStatus.ACTIVE),
                new BigDecimal("100.00"),
                "PEN",
                status);

        ReflectionTestUtils.setField(payment, "id", id);
        ReflectionTestUtils.setField(payment, "createdAt", CREATED_AT);
        ReflectionTestUtils.setField(payment, "updatedAt", UPDATED_AT);

        return payment;
    }
}
