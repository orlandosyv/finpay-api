package com.finpay.api.dto;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, String> fieldErrors) {

    public ApiError(
            Instant timestamp,
            int status,
            String error,
            String message,
            String path) {
        this(timestamp, status, error, message, path, Map.of());
    }
}
