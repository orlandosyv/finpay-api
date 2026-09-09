package com.finpay.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must have a valid format")
    @Size(max = 254, message = "Email must not exceed 254 characters")
    @Schema(example = "admin@tienda.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(max = 72, message = "Password must not exceed 72 characters")
    @Schema(example = "StrongPassword123!")
    private String password;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
