package com.serviceplus.filemanagement.controller;

import com.serviceplus.filemanagement.ExceptionHandler.SPRuntimeError;
import com.serviceplus.filemanagement.dto.CreateUploadSessionsRequest;
import com.serviceplus.filemanagement.dto.CreateUploadSessionsResponse;
import com.serviceplus.filemanagement.dto.ReplaceFileRequest;
import com.serviceplus.filemanagement.dto.ReplaceFileResponse;
import com.serviceplus.filemanagement.model.UserSessionObject;
import com.serviceplus.filemanagement.model.persistenceEntity.UploadSession;
import com.serviceplus.filemanagement.service.UploadSessionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Objects;

import static com.serviceplus.filemanagement.Utility.ApplicationConstants.UPLOAD_STATUS_INITIATED;
import static com.serviceplus.filemanagement.Utility.Utility.AESEncrypt;
import static com.serviceplus.filemanagement.Utility.Utility.getUserSessionDetails;

@RestController
@RequestMapping("/b")
public class UploadSessionController {

    @Autowired
    private UploadSessionService uploadSessionService;

    @PostMapping("/upload-sessions")
    public CreateUploadSessionsResponse createSessions(
            @RequestBody CreateUploadSessionsRequest request, HttpServletRequest httpRequest
            ) {
        return uploadSessionService.createSessions(request,httpRequest);
    }
}
