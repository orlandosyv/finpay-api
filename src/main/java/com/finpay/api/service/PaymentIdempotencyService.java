package com.finpay.api.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.finpay.api.context.CurrentMerchantProvider;
import com.finpay.api.dto.CreatePaymentRequest;
import com.finpay.api.dto.IdempotentPaymentResult;
import com.finpay.api.exception.IdempotencyConflictException;
import com.finpay.api.exception.InvalidIdempotencyKeyException;
import com.finpay.api.model.PaymentIdempotency;
import com.finpay.api.repository.PaymentIdempotencyRepository;

@Service
public class PaymentIdempotencyService {

    private static final int MAX_KEY_LENGTH = 128;

    private final CurrentMerchantProvider currentMerchantProvider;
    private final PaymentIdempotencyRepository idempotencyRepository;
    private final PaymentIdempotencyTransactionService transactionService;

    public PaymentIdempotencyService(
            CurrentMerchantProvider currentMerchantProvider,
            PaymentIdempotencyRepository idempotencyRepository,
            PaymentIdempotencyTransactionService transactionService) {
        this.currentMerchantProvider = currentMerchantProvider;
        this.idempotencyRepository = idempotencyRepository;
        this.transactionService = transactionService;
    }

    @PreAuthorize("hasAnyRole('MERCHANT_ADMIN', 'MERCHANT_USER')")
    public IdempotentPaymentResult createPayment(
            String suppliedKey,
            CreatePaymentRequest request) {
        String idempotencyKey = normalizeKey(suppliedKey);
        String requestHash = hashRequest(request);
        Long merchantId = currentMerchantProvider.getCurrentMerchantId();

        var existing = idempotencyRepository
                .findByMerchantIdAndIdempotencyKey(merchantId, idempotencyKey);
        if (existing.isPresent()) {
            return replay(existing.get(), requestHash);
        }

        try {
            return transactionService.create(idempotencyKey, requestHash, request);
        } catch (DataIntegrityViolationException exception) {
            // A concurrent request may have inserted the same merchant/key first.
            PaymentIdempotency winner = idempotencyRepository
                    .findByMerchantIdAndIdempotencyKey(merchantId, idempotencyKey)
                    .orElseThrow(() -> exception);
            return replay(winner, requestHash);
        }
    }

    private IdempotentPaymentResult replay(
            PaymentIdempotency existing,
            String requestHash) {
        if (!MessageDigest.isEqual(
                existing.getRequestHash().getBytes(StandardCharsets.US_ASCII),
                requestHash.getBytes(StandardCharsets.US_ASCII))) {
            throw new IdempotencyConflictException();
        }

        return new IdempotentPaymentResult(existing.toOriginalResponse(), true);
    }

    private String normalizeKey(String suppliedKey) {
        if (suppliedKey == null || suppliedKey.isBlank()) {
            throw new InvalidIdempotencyKeyException("Idempotency-Key header is required");
        }

        String normalized = suppliedKey.trim();
        if (normalized.length() > MAX_KEY_LENGTH) {
            throw new InvalidIdempotencyKeyException(
                    "Idempotency-Key must not exceed 128 characters");
        }
        return normalized;
    }

    private String hashRequest(CreatePaymentRequest request) {
        String canonicalRequest = request.getAmount()
                .stripTrailingZeros()
                .toPlainString()
                + "|"
                + request.getCurrency();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonicalRequest.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
