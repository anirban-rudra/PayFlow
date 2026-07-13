package com.paypal.transaction_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CreateTransactionRequest {
    @NotBlank
    @Size(min = 4, max = 31)
    @Pattern(regexp = "^@[A-Za-z0-9][A-Za-z0-9._-]{2,29}$", message = "Receiver PayTag must start with @")
    private String receiverPayTag;

    @NotNull
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    @Digits(integer = 17, fraction = 2)
    private BigDecimal amount;

    public String getReceiverPayTag() {
        return receiverPayTag;
    }

    public void setReceiverPayTag(String receiverPayTag) {
        this.receiverPayTag = receiverPayTag;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
