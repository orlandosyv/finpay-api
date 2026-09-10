package com.finpay.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class WebhookRetryPolicyTest {

    private final WebhookRetryPolicy retryPolicy = new WebhookRetryPolicy();

    @Test
    void appliesBackoffAndStopsAfterFiveAttempts() {
        assertThat(retryPolicy.delayAfterFailure(1)).isEqualTo(Duration.ofMinutes(1));
        assertThat(retryPolicy.delayAfterFailure(2)).isEqualTo(Duration.ofMinutes(5));
        assertThat(retryPolicy.delayAfterFailure(3)).isEqualTo(Duration.ofMinutes(30));
        assertThat(retryPolicy.delayAfterFailure(4)).isEqualTo(Duration.ofHours(2));
        assertThat(retryPolicy.hasAnotherAttempt(4)).isTrue();
        assertThat(retryPolicy.hasAnotherAttempt(5)).isFalse();
    }
}
