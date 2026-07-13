package com.paypal.transaction_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;



@Entity
@Table(
        name = "transactions",
        uniqueConstraints = @UniqueConstraint(name = "uk_transactions_sender_idempotency", columnNames = {"sender_id", "idempotency_key"})
)

public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_reference", length = 32, unique = true)
    @Size(max = 32)
    private String publicReference;

    @Column(name = "sender_id", nullable = false)
    @NotNull
    private Long senderId;

    @Column(name = "receiver_id", nullable = false)
    @NotNull
    private Long receiverId;

    @Column(name = "receiver_pay_tag", length = 40)
    @Size(max = 40)
    private String receiverPayTag;

    @Column(name = "sender_pay_tag", length = 40)
    @Size(max = 40)
    private String senderPayTag;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    @NotNull
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    @Digits(integer = 17, fraction = 2)
    private BigDecimal amount;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "hold_reference", length = 128)
    @Size(max = 128)
    private String holdReference;

    @Column(name = "idempotency_key", length = 128)
    @Size(max = 128)
    private String idempotencyKey;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public Transaction() {}

    public Transaction(Long senderId, Long receiverId,
                       String senderNameSnapshot, String receiverNameSnapshot,
                       BigDecimal amount, LocalDateTime timestamp, String status) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.amount = amount;
        this.timestamp = timestamp;
        this.status = status;
    }

    @PrePersist
    public void prePersist() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        if (publicReference == null || publicReference.isBlank()) {
            publicReference = "PF-TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        }
        if (updatedAt == null) {
            updatedAt = timestamp;
        }
        if (status == null) {
            status = TransactionStatus.CREATED.name();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPublicReference() {
        return publicReference;
    }

    public void setPublicReference(String publicReference) {
        this.publicReference = publicReference;
    }

    public Long getSenderId() {
        return senderId;
    }
    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public Long getReceiverId() {
        return receiverId;
    }
    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public String getReceiverPayTag() {
        return receiverPayTag;
    }

    public void setReceiverPayTag(String receiverPayTag) {
        this.receiverPayTag = receiverPayTag;
    }

    public String getSenderPayTag() {
        return senderPayTag;
    }

    public void setSenderPayTag(String senderPayTag) {
        this.senderPayTag = senderPayTag;
    }

    public BigDecimal getAmount() {
        return amount;
    }
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status.name();
    }

    public String getHoldReference() {
        return holdReference;
    }

    public void setHoldReference(String holdReference) {
        this.holdReference = holdReference;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", publicReference='" + publicReference + '\'' +
                ", senderId=" + senderId +
                ", receiverId=" + receiverId +
                ", senderPayTag='" + senderPayTag + '\'' +
                ", receiverPayTag='" + receiverPayTag + '\'' +
                ", amount=" + amount +
                ", timestamp=" + timestamp +
                ", status='" + status + '\'' +
                ", holdReference='" + holdReference + '\'' +
                '}';
    }
}
