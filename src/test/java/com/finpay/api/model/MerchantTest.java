package com.finpay.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MerchantTest {

    @Test
    void createsActiveMerchant() {
        Merchant merchant = new Merchant("Test Merchant", MerchantStatus.ACTIVE);

        assertThat(merchant.getId()).isNull();
        assertThat(merchant.getName()).isEqualTo("Test Merchant");
        assertThat(merchant.getStatus()).isEqualTo(MerchantStatus.ACTIVE);
        assertThat(merchant.getCreatedAt()).isNull();
        assertThat(merchant.getUpdatedAt()).isNull();
    }
}
