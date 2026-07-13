package com.paypal.transaction_service.dto;

import java.math.BigDecimal;

public class TransactionResponse {
    private Long id;
    private String publicReference;
    private Long senderId;
    private Long receiverId;
    private String senderPayTag;
    private String receiverPayTag;
    private BigDecimal amount;
    private String timestamp;
    private String status;
    private String message;
    private String holdReference;
    private String failureReason;
    private String completedAt;

    // constructors, getters, setters
    public TransactionResponse() {}

    public TransactionResponse(Long id, Long senderId, Long receiverId, BigDecimal amount,
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

    public String getPublicReference() { return publicReference; }
    public void setPublicReference(String publicReference) { this.publicReference = publicReference; }

    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }

    public String getSenderPayTag() { return senderPayTag; }
    public void setSenderPayTag(String senderPayTag) { this.senderPayTag = senderPayTag; }

    public String getReceiverPayTag() { return receiverPayTag; }
    public void setReceiverPayTag(String receiverPayTag) { this.receiverPayTag = receiverPayTag; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getHoldReference() { return holdReference; }
    public void setHoldReference(String holdReference) { this.holdReference = holdReference; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public String getCompletedAt() { return completedAt; }
    public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }
}
