package com.paypal.user_service.dto;

import com.paypal.user_service.entity.User;

public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String payTag;
    private String role;

    public UserResponse() {
    }

    public UserResponse(Long id, String name, String email, String payTag, String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.payTag = payTag;
        this.role = role;
    }

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getPayTag(), user.getRole());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPayTag() {
        return payTag;
    }

    public void setPayTag(String payTag) {
        this.payTag = payTag;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
