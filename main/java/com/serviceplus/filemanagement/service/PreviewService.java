package com.serviceplus.filemanagement.service;

public interface PreviewService {
    void generatePreview(String fileId);
    String getPreviewUrl(String fileId);
}
