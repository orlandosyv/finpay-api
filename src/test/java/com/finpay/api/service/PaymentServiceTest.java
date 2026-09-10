package com.finpay.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.finpay.api.context.CurrentMerchantProvider;
import com.finpay.api.dto.CreatePaymentRequest;
import com.finpay.api.exception.InvalidPaymentStatusTransitionException;
import com.finpay.api.exception.MerchantNotFoundException;
import com.finpay.api.exception.PaymentNotFoundException;
import com.finpay.api.model.Merchant;
import com.finpay.api.model.MerchantStatus;
import com.finpay.api.model.Payment;
import com.finpay.api.model.PaymentStatus;
import com.finpay.api.repository.MerchantRepository;
import com.finpay.api.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final Long MERCHANT_ID = 1L;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private CurrentMerchantProvider currentMerchantProvider;

    @Mock
    private PaymentEventService paymentEventService;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        when(currentMerchantProvider.getCurrentMerchantId()).thenReturn(MERCHANT_ID);
    }

    @Test
    void createsPaymentWithPendingStatus() {
        Merchant merchant = merchant();
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setAmount(new BigDecimal("250.00"));
        request.setCurrency("PEN");

        when(merchantRepository.findById(MERCHANT_ID)).thenReturn(Optional.of(merchant));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = paymentService.createPayment(request);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());

        Payment savedPayment = paymentCaptor.getValue();
        assertThat(result).isSameAs(savedPayment);
        assertThat(savedPayment.getAmount()).isEqualByComparingTo("250.00");
        assertThat(savedPayment.getCurrency()).isEqualTo("PEN");
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(savedPayment.getMerchant()).isSameAs(merchant);
        verify(merchantRepository).findById(MERCHANT_ID);
    }

    @Test
    void rejectsCreationWhenCurrentMerchantDoesNotExist() {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setAmount(new BigDecimal("250.00"));
        request.setCurrency("PEN");
        when(merchantRepository.findById(MERCHANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.createPayment(request))
                .isInstanceOf(MerchantNotFoundException.class)
                .hasMessage("Merchant with id 1 was not found");

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void returnsAllPayments() {
        List<Payment> payments = List.of(
                paymentWithStatus(PaymentStatus.PENDING),
                paymentWithStatus(PaymentStatus.APPROVED));
        when(paymentRepository.findAllByMerchantId(MERCHANT_ID)).thenReturn(payments);

        List<Payment> result = paymentService.getAllPayments();

        assertThat(result).containsExactlyElementsOf(payments);
        verify(paymentRepository).findAllByMerchantId(MERCHANT_ID);
    }

    @Test
    void returnsPaymentWhenIdExists() {
        Payment payment = paymentWithStatus(PaymentStatus.PENDING);
        when(paymentRepository.findByIdAndMerchantId(1L, MERCHANT_ID))
                .thenReturn(Optional.of(payment));

        Payment result = paymentService.getPaymentById(1L);

        assertThat(result).isSameAs(payment);
        verify(paymentRepository).findByIdAndMerchantId(1L, MERCHANT_ID);
    }

    @Test
    void throwsPaymentNotFoundWhenIdDoesNotExist() {
        when(paymentRepository.findByIdAndMerchantId(999L, MERCHANT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentById(999L))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessage("Payment with id 999 was not found");

        verify(paymentRepository).findByIdAndMerchantId(999L, MERCHANT_ID);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void approvesPendingPaymentAndSavesIt() {
        Payment payment = paymentWithStatus(PaymentStatus.PENDING);
        when(paymentRepository.findByIdAndMerchantId(1L, MERCHANT_ID))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        Payment result = paymentService.approvePayment(1L);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        verify(paymentRepository).findByIdAndMerchantId(1L, MERCHANT_ID);
        verify(paymentRepository).save(payment);
    }

    @Test
    void declinesPendingPaymentAndSavesIt() {
        Payment payment = paymentWithStatus(PaymentStatus.PENDING);
        when(paymentRepository.findByIdAndMerchantId(1L, MERCHANT_ID))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        Payment result = paymentService.declinePayment(1L);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.DECLINED);
        verify(paymentRepository).findByIdAndMerchantId(1L, MERCHANT_ID);
        verify(paymentRepository).save(payment);
    }

    @Test
    void refundsApprovedPaymentAndSavesIt() {
        Payment payment = paymentWithStatus(PaymentStatus.APPROVED);
        when(paymentRepository.findByIdAndMerchantId(1L, MERCHANT_ID))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        Payment result = paymentService.refundPayment(1L);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(paymentRepository).findByIdAndMerchantId(1L, MERCHANT_ID);
        verify(paymentRepository).save(payment);
    }

    @Test
    void doesNotSavePaymentWhenTransitionIsInvalid() {
        Payment payment = paymentWithStatus(PaymentStatus.DECLINED);
        when(paymentRepository.findByIdAndMerchantId(1L, MERCHANT_ID))
                .thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.refundPayment(1L))
                .isInstanceOf(InvalidPaymentStatusTransitionException.class)
                .hasMessage("Payment cannot transition from DECLINED to REFUNDED");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.DECLINED);
        verify(paymentRepository).findByIdAndMerchantId(1L, MERCHANT_ID);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    private Payment paymentWithStatus(PaymentStatus status) {
        return new Payment(
                merchant(),
                new BigDecimal("100.00"),
                "PEN",
                status);
    }

    private Merchant merchant() {
        return new Merchant("Test Merchant", MerchantStatus.ACTIVE);
    }
}
