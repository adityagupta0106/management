package com.serviceplus.filemanagement.service;

import com.serviceplus.filemanagement.dto.CancelResult;
import com.serviceplus.filemanagement.dto.CreateUploadSessionsRequest;
import com.serviceplus.filemanagement.dto.CreateUploadSessionsResponse;
import com.serviceplus.filemanagement.dto.ReplaceFileRequest;
import com.serviceplus.filemanagement.model.persistenceEntity.UploadSession;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

public interface UploadSessionService {

    UploadSession getSession(String uploadId,String tenantId);

    void markInProgress(String uploadId, String tenantId);

    void markCompleted(String uploadId, String tenantId);

    CreateUploadSessionsResponse createSessions(CreateUploadSessionsRequest request, HttpServletRequest httpRequest);

    CancelResult cancelUpload(String uploadId, HttpServletRequest request);

    UploadSession findByPublicUploadRef(String publicUploadRef, String tenantId);

    void save(UploadSession uploadSession);

    ResponseEntity<?> replaceSession(HttpServletRequest request, String publicUploadRef);

}
