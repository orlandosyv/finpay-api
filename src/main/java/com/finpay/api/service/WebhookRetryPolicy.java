package com.finpay.api.service;

import java.time.Duration;

import org.springframework.stereotype.Component;

@Component
public class WebhookRetryPolicy {

    private static final int MAX_ATTEMPTS = 5;

    public boolean hasAnotherAttempt(int completedAttempts) {
        return completedAttempts < MAX_ATTEMPTS;
    }

    public Duration delayAfterFailure(int completedAttempts) {
        return switch (completedAttempts) {
            case 1 -> Duration.ofMinutes(1);
            case 2 -> Duration.ofMinutes(5);
            case 3 -> Duration.ofMinutes(30);
            default -> Duration.ofHours(2);
        };
    }
}
