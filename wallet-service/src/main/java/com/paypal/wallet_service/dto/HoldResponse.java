package com.paypal.wallet_service.dto;

import java.math.BigDecimal;

public class HoldResponse {
    private String holdReference;
    private BigDecimal amount;
    private String status;

    public HoldResponse(String holdReference, BigDecimal amount, String status) {
        this.holdReference = holdReference;
        this.amount = amount;
        this.status = status;
    }

    public String getHoldReference() { return holdReference; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
}
