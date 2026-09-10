package com.finpay.api.service;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finpay.api.context.CurrentMerchantProvider;
import com.finpay.api.dto.CreateWebhookEndpointRequest;
import com.finpay.api.dto.WebhookEndpointCreatedResponse;
import com.finpay.api.dto.WebhookEndpointResponse;
import com.finpay.api.exception.MerchantNotFoundException;
import com.finpay.api.exception.WebhookEndpointNotFoundException;
import com.finpay.api.model.Merchant;
import com.finpay.api.model.WebhookEndpoint;
import com.finpay.api.repository.MerchantRepository;
import com.finpay.api.repository.WebhookEndpointRepository;

@Service
public class WebhookEndpointService {

    private final CurrentMerchantProvider currentMerchantProvider;
    private final MerchantRepository merchantRepository;
    private final WebhookEndpointRepository webhookEndpointRepository;
    private final WebhookUrlValidator urlValidator;
    private final WebhookSecretCrypto secretCrypto;

    public WebhookEndpointService(
            CurrentMerchantProvider currentMerchantProvider,
            MerchantRepository merchantRepository,
            WebhookEndpointRepository webhookEndpointRepository,
            WebhookUrlValidator urlValidator,
            WebhookSecretCrypto secretCrypto) {
        this.currentMerchantProvider = currentMerchantProvider;
        this.merchantRepository = merchantRepository;
        this.webhookEndpointRepository = webhookEndpointRepository;
        this.urlValidator = urlValidator;
        this.secretCrypto = secretCrypto;
    }

    @PreAuthorize("hasRole('MERCHANT_ADMIN')")
    @Transactional
    public WebhookEndpointCreatedResponse create(CreateWebhookEndpointRequest request) {
        Long merchantId = currentMerchantProvider.getCurrentMerchantId();
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException(merchantId));
        String normalizedUrl = urlValidator.validateAndNormalize(request.getUrl());
        String signingSecret = secretCrypto.generateSigningSecret();

        WebhookEndpoint endpoint = webhookEndpointRepository.saveAndFlush(
                new WebhookEndpoint(
                        merchant,
                        normalizedUrl,
                        secretCrypto.encrypt(signingSecret)));
        return new WebhookEndpointCreatedResponse(
                endpoint.getId(),
                endpoint.getUrl(),
                endpoint.isActive(),
                signingSecret,
                endpoint.getCreatedAt(),
                endpoint.getUpdatedAt());
    }

    @PreAuthorize("hasRole('MERCHANT_ADMIN')")
    @Transactional(readOnly = true)
    public List<WebhookEndpointResponse> getActiveEndpoints() {
        return webhookEndpointRepository
                .findAllByMerchantIdAndActiveTrueOrderByIdAsc(
                        currentMerchantProvider.getCurrentMerchantId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PreAuthorize("hasRole('MERCHANT_ADMIN')")
    @Transactional
    public void delete(Long endpointId) {
        Long merchantId = currentMerchantProvider.getCurrentMerchantId();
        WebhookEndpoint endpoint = webhookEndpointRepository
                .findByIdAndMerchantIdAndActiveTrue(endpointId, merchantId)
                .orElseThrow(() -> new WebhookEndpointNotFoundException(endpointId));
        endpoint.deactivate();
    }

    private WebhookEndpointResponse toResponse(WebhookEndpoint endpoint) {
        return new WebhookEndpointResponse(
                endpoint.getId(),
                endpoint.getUrl(),
                endpoint.isActive(),
                endpoint.getCreatedAt(),
                endpoint.getUpdatedAt());
    }
}
