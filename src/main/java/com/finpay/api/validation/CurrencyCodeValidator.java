package com.finpay.api.validation;

import java.util.Currency;
import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CurrencyCodeValidator implements ConstraintValidator<ValidCurrency, String> {

    private static final Pattern CURRENCY_CODE_PATTERN = Pattern.compile("^[A-Z]{3}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        if (!CURRENCY_CODE_PATTERN.matcher(value).matches()) {
            return false;
        }

        try {
            Currency.getInstance(value);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
