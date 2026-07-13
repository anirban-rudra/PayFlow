package com.paypal.user_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class SignupRequest {
    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @NotBlank
    @Size(min = 4, max = 31)
    @Pattern(regexp = "^@[A-Za-z0-9][A-Za-z0-9._-]{2,29}$", message = "PayTag must start with @ and contain 3-30 letters, numbers, dots, underscores, or hyphens")
    private String payTag;

    @NotBlank
    @Size(min = 8, max = 128)
    private String password;

    private String adminKey;

    public SignupRequest() {
        // default constructor
    }

    public SignupRequest(String name, String email, String password, String adminKey) {
        this(name, email, null, password, adminKey);
    }

    public SignupRequest(String name, String email, String payTag, String password, String adminKey) {
        this.name = name;
        this.email = email;
        this.payTag = payTag;
        this.password = password;
        this.adminKey = adminKey;

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAdminKey() {
        return adminKey;
    }

    public void setAdminKey(String adminKey) {
        this.adminKey = adminKey;
    }
}
