package com.serviceplus.filemanagement.service;

import com.serviceplus.filemanagement.dto.UploadStatusResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.io.InputStream;

public interface UploadService {

    UploadStatusResponse uploadChunk(HttpServletRequest request, String uploadId, int chunkNumber, InputStream data, int totalChunks);
}
