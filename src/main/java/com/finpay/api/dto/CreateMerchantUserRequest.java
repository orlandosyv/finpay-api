package com.finpay.api.dto;

import com.finpay.api.model.MerchantRole;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Data required to create a user in the authenticated merchant")
public class CreateMerchantUserRequest {

    @Schema(description = "User email address", example = "operator@tienda.com")
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 254, message = "Email must have at most 254 characters")
    private String email;

    @Schema(description = "Initial password", example = "OperatorPassword123!")
    @NotBlank(message = "Password is required")
    @Size(
            min = 12,
            max = 72,
            message = "Password must have between 12 and 72 characters")
    @Pattern(
            regexp = "^(?=\\S+$)(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).*$",
            message = "Password must include uppercase, lowercase, number and special character")
    private String password;

    @Schema(
            description = "Role granted inside the authenticated merchant",
            example = "MERCHANT_USER")
    @NotNull(message = "Role is required")
    private MerchantRole role;

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

    public MerchantRole getRole() {
        return role;
    }

    public void setRole(MerchantRole role) {
        this.role = role;
    }
}
