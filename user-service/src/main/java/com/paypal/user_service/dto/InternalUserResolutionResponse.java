package com.paypal.user_service.dto;

import com.paypal.user_service.entity.User;

public class InternalUserResolutionResponse {
    private Long userId;
    private String displayName;
    private String payTag;

    public InternalUserResolutionResponse() {
    }

    public InternalUserResolutionResponse(Long userId, String displayName, String payTag) {
        this.userId = userId;
        this.displayName = displayName;
        this.payTag = payTag;
    }

    public static InternalUserResolutionResponse from(User user) {
        return new InternalUserResolutionResponse(user.getId(), user.getName(), user.getPayTag());
    }

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
