package com.paypal.transaction_service.dto.dto;


import java.math.BigDecimal;

public class HoldRequest {
    private Long userId;
    private String currency;
    private BigDecimal amount;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
