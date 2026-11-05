package com.serviceplus.filemanagement.dto;

public class SignedUrlResponse {

    private String url;
    private Long expiryInSecond;
    private String uploadId;

    public SignedUrlResponse(String url, long expiry,String uploadId) {
        this.url = url;
        this.expiryInSecond = expiry;
        this.uploadId = uploadId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Long getExpiryInSecond() {
        return expiryInSecond;
    }

    public void setExpiryInSecond(Long expiryInSecond) {
        this.expiryInSecond = expiryInSecond;
    }

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }
}
