package com.paypal.reward_service.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reward")
public class Reward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal points;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @Column(nullable = false, unique = true)
    private Long transactionId;

    //getter setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public BigDecimal getPoints() { return points; }
    public void setPoints(BigDecimal points) { this.points = points; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime timestamp) { this.sentAt = timestamp; }

    public void setTransactionId(Long id) {
        transactionId = id;
    }

    public Long getTransactionId() {
        return transactionId;
    }

}
