package com.paypal.transaction_service.dto.dto;

import java.math.BigDecimal;

public class HoldResponse {
    private String holdReference;
    private BigDecimal amount;
    private String status;

    public HoldResponse() {
    }

    public HoldResponse(String holdReference, BigDecimal amount, String status) {
        this.holdReference = holdReference;
        this.amount = amount;
        this.status = status;
    }

    public String getHoldReference() { return holdReference; }
    public void setHoldReference(String holdReference) { this.holdReference = holdReference; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
