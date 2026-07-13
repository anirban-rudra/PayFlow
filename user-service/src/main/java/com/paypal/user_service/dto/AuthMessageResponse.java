package com.paypal.user_service.dto;

public class AuthMessageResponse {
    private String message;
    private Long userId;

    public AuthMessageResponse() {
    }

    public AuthMessageResponse(String message, Long userId) {
        this.message = message;
        this.userId = userId;
    }

    public AuthMessageResponse(String message) {
        this(message, null);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
