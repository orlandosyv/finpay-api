package com.finpay.api.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.finpay.api.context.CurrentMerchantProvider;
import com.finpay.api.dto.CreatePaymentRequest;
import com.finpay.api.exception.MerchantNotFoundException;
import com.finpay.api.model.Merchant;
import com.finpay.api.model.Payment;
import com.finpay.api.repository.MerchantRepository;
import com.finpay.api.repository.PaymentRepository;
import com.finpay.api.model.PaymentStatus;
import com.finpay.api.exception.PaymentNotFoundException;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final MerchantRepository merchantRepository;
    private final CurrentMerchantProvider currentMerchantProvider;

    public PaymentService(
            PaymentRepository paymentRepository,
            MerchantRepository merchantRepository,
            CurrentMerchantProvider currentMerchantProvider) {
        this.paymentRepository = paymentRepository;
        this.merchantRepository = merchantRepository;
        this.currentMerchantProvider = currentMerchantProvider;
    }

    @Transactional(readOnly = true)
    public List<Payment> getAllPayments() {
        return paymentRepository.findAllByMerchantId(currentMerchantId());
    }

    @Transactional(readOnly = true)
    public Payment getPaymentById(Long id) {
        return findPaymentOrThrow(id);
    }

    @Transactional
    public Payment createPayment(CreatePaymentRequest request) {
        Long merchantId = currentMerchantId();
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException(merchantId));

        Payment payment = new Payment(
                merchant,
                request.getAmount(),
                request.getCurrency(),
                PaymentStatus.PENDING);

        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment approvePayment(Long id) {
        Payment payment = findPaymentOrThrow(id);

        payment.approve();

        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment declinePayment(Long id) {
        Payment payment = findPaymentOrThrow(id);

        payment.decline();

        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment refundPayment(Long id) {
        Payment payment = findPaymentOrThrow(id);

        payment.refund();

        return paymentRepository.save(payment);
    }

    private Payment findPaymentOrThrow(Long id) {
        return paymentRepository.findByIdAndMerchantId(id, currentMerchantId())
                .orElseThrow(() -> new PaymentNotFoundException(id));
    }

    private Long currentMerchantId() {
        return currentMerchantProvider.getCurrentMerchantId();
    }

}
