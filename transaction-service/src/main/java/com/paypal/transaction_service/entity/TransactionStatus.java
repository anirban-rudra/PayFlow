package com.paypal.transaction_service.entity;

public enum TransactionStatus {
    CREATED,
    HOLD_PLACED,
    CAPTURED,
    CREDITED,
    SUCCESS,
    REFUND_PENDING,
    REFUNDED,
    MANUAL_REVIEW,
    FAILED
}
