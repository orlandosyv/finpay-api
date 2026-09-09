package com.finpay.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Data required to register a merchant and its first administrator")
public class RegisterMerchantRequest {

    @Schema(description = "Merchant display name", example = "Tienda Andina")
    @NotBlank(message = "Merchant name is required")
    @Size(max = 150, message = "Merchant name must have at most 150 characters")
    private String merchantName;

    @Schema(description = "Administrator email address", example = "admin@tienda.com")
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 254, message = "Email must have at most 254 characters")
    private String email;

    @Schema(description = "Administrator password", example = "StrongPassword123!")
    @NotBlank(message = "Password is required")
    @Size(
            min = 12,
            max = 72,
            message = "Password must have between 12 and 72 characters")
    @Pattern(
            regexp = "^(?=\\S+$)(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).*$",
            message = "Password must include uppercase, lowercase, number and special character")
    private String password;

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

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
