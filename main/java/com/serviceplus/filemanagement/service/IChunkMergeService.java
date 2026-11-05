package com.serviceplus.filemanagement.service;

import com.serviceplus.filemanagement.model.UploadToken;
import com.serviceplus.filemanagement.model.UserSessionObject;
import jakarta.servlet.http.HttpServletRequest;

import java.io.InputStream;

public interface IChunkMergeService {

    InputStream mergeChunks(HttpServletRequest request, String uploadId, int totalChunks);

    void mergeToFinalObject(HttpServletRequest request,String bucket,String objectKey,String uploadId, int totalChunks, UserSessionObject user,
                            UploadToken payload);

    void deleteDataChunkValidationFailure(String uploadId, Integer totalChunks);

}
