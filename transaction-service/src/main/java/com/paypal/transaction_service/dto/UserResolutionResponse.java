package com.paypal.transaction_service.dto;

public class UserResolutionResponse {
    private Long userId;
    private String displayName;
    private String payTag;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPayTag() {
        return payTag;
    }

    public void setPayTag(String payTag) {
        this.payTag = payTag;
    }
}
