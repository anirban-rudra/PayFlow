package com.paypal.user_service.dto;

import com.paypal.user_service.entity.User;

public class TransferTargetResponse {
    private String displayName;
    private String payTag;

    public TransferTargetResponse() {
    }

    public TransferTargetResponse(String displayName, String payTag) {
        this.displayName = displayName;
        this.payTag = payTag;
    }

    public static TransferTargetResponse from(User user) {
        return new TransferTargetResponse(user.getName(), user.getPayTag());
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
