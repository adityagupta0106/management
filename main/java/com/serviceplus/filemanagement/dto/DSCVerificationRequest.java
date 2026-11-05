package com.serviceplus.filemanagement.dto;

public class DSCVerificationRequest {

    private Long userId;

    public DSCVerificationRequest() {
    }

    public DSCVerificationRequest(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
