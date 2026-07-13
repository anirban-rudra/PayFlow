package com.paypal.wallet_service.dto;


import jakarta.validation.constraints.NotBlank;

public class CaptureRequest {
    @NotBlank
    private String holdReference;

    public String getHoldReference() { return holdReference; }
    public void setHoldReference(String holdReference) { this.holdReference = holdReference; }
}
