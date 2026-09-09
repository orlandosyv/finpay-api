package com.finpay.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Refresh token belonging to the session being closed")
public class LogoutRequest {

    @Schema(description = "Refresh token returned by the latest login or refresh")
    @NotBlank(message = "Refresh token is required")
    @Size(max = 512, message = "Refresh token must have at most 512 characters")
    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
