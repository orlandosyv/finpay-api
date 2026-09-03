package com.finpay.api.model;

import java.math.BigDecimal;

public class Payment {

    private Long id;
    private BigDecimal amount;
    private String currency;
    private String status;

    public Payment(Long id, BigDecimal amount,
                String currency, String status) {
        this.id = id;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getStatus() {
        return status;
    }
}
