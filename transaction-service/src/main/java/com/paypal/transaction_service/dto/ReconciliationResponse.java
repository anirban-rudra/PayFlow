package com.paypal.transaction_service.dto;

public class ReconciliationResponse {
    private Long transactionId;
    private String previousStatus;
    private String currentStatus;
    private String action;
    private String message;

    public ReconciliationResponse() {
    }

    public ReconciliationResponse(Long transactionId, String previousStatus, String currentStatus, String action, String message) {
        this.transactionId = transactionId;
        this.previousStatus = previousStatus;
        this.currentStatus = currentStatus;
        this.action = action;
        this.message = message;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
