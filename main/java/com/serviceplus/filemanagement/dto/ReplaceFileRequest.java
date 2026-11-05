package com.serviceplus.filemanagement.dto;

import java.util.List;

public class ReplaceFileRequest {

    private String fileName;

    private Long maxFileSize;

    private List<String> allowedMime;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(Long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public List<String> getAllowedMime() {
        return allowedMime;
    }

    public void setAllowedMime(List<String> allowedMime) {
        this.allowedMime = allowedMime;
    }
}
