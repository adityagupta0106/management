package com.serviceplus.filemanagement.dto;

public class ReplaceFileResponse {

    private String oldUploadId;

    private String newUploadId;

    private String uploadToken;

    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUploadToken() {
        return uploadToken;
    }

    public void setUploadToken(String uploadToken) {
        this.uploadToken = uploadToken;
    }

    public String getOldUploadId() {
        return oldUploadId;
    }

    public void setOldUploadId(String oldUploadId) {
        this.oldUploadId = oldUploadId;
    }

    public String getNewUploadId() {
        return newUploadId;
    }

    public void setNewUploadId(String newUploadId) {
        this.newUploadId = newUploadId;
    }
}
