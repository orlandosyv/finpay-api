package com.finpay.api.context;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DefaultMerchantProvider implements CurrentMerchantProvider {

    private final Long defaultMerchantId;

    public DefaultMerchantProvider(
            @Value("${finpay.merchant.default-id}") Long defaultMerchantId) {
        this.defaultMerchantId = defaultMerchantId;
    }

    @Override
    public Long getCurrentMerchantId() {
        return defaultMerchantId;
    }
}
