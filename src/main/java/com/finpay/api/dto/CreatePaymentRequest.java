package com.finpay.api.dto;

import java.math.BigDecimal;

import com.finpay.api.validation.ValidCurrency;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Data required to create a payment")
public class CreatePaymentRequest {

    @Schema(description = "Positive payment amount with at most two decimal places", example = "125.50")
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    @Digits(integer = 17, fraction = 2, message = "Amount must have at most 17 integer digits and 2 decimal places")
    private BigDecimal amount;

    @Schema(description = "Uppercase ISO 4217 currency code", example = "PEN")
    @NotBlank(message = "Currency is required")
    @ValidCurrency
    private String currency;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
