package com.paypal.wallet_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateWalletRequest {
    @NotNull
    private Long userId;

    @Size(min = 3, max = 3)
    private String currency;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
