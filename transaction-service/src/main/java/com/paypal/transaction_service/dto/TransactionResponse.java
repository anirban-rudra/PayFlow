package com.paypal.transaction_service.dto;

public class TransactionResponse {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private Double amount;
    private String timestamp;
    private String status;
    private String message;

    // constructors, getters, setters
    public TransactionResponse() {}

    public TransactionResponse(Long id, Long senderId, Long receiverId, Double amount,
                               String timestamp, String status, String message) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.amount = amount;
        this.timestamp = timestamp;
        this.status = status;
        this.message = message;
    }

    // getters and setters for all fields
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}