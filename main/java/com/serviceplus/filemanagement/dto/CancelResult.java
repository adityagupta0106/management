package com.serviceplus.filemanagement.dto;

public class CancelResult {

    private String uploadId;
    private String status;
    private String message;

    public CancelResult() {
    }

    public CancelResult(String uploadId, String status, String message) {
        this.uploadId = uploadId;
        this.status = status;
        this.message = message;
    }

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
