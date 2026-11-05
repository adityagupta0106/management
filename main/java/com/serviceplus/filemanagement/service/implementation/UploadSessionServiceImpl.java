package com.serviceplus.filemanagement.service.implementation;

import com.serviceplus.filemanagement.ExceptionHandler.SPRuntimeError;
import com.serviceplus.filemanagement.Utility.FileUploadJWTService;
import com.serviceplus.filemanagement.dto.*;
import com.serviceplus.filemanagement.model.UserSessionObject;
import com.serviceplus.filemanagement.model.persistenceEntity.DocumentMetadata;
import com.serviceplus.filemanagement.model.persistenceEntity.UploadSession;
import com.serviceplus.filemanagement.repository.UploadSessionRepository;
import com.serviceplus.filemanagement.service.UploadSessionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.serviceplus.filemanagement.Utility.ApplicationConstants.*;
import static com.serviceplus.filemanagement.Utility.Utility.*;
import static java.util.Objects.isNull;

@Service
public class UploadSessionServiceImpl implements UploadSessionService {

    @Autowired
    private UploadSessionRepository repository;

    @Autowired
    private FileUploadJWTService fileUploadJWTService;

    @Value("${upload.session.chunk.split.mb}")
    private Integer uploadSessionChunkSplitMB;

    @Autowired
    private DocumentMetaDataService  documentMetaDataService;

    @Override
    public void save(UploadSession uploadSession){
        repository.save(uploadSession);
    }

    @Override
    public UploadSession getSession(String uploadId,String tenantId) {
       return repository.findByUploadIdAndTenantId(uploadId,tenantId)
       // return repository.findById(uploadId)
                .orElseThrow(() ->
                        new SPRuntimeError("Upload session not found", HttpStatus.NOT_FOUND));
    }

    @Override
    public void markInProgress(String uploadId, String tenantId) {
        UploadSession session = findByPublicUploadRef(uploadId,tenantId);

        if (UPLOAD_STATUS_INITIATED.equals(session.getStatus())) {
            session.setStatus(UPLOAD_STATUS_IN_PROGRESS);
            save(session);
        }
    }

    @Override
    public void markCompleted(String uploadId, String tenantId) {
        UploadSession session = findByPublicUploadRef(uploadId,tenantId);

        session.setStatus(UPLOAD_STATUS_COMPLETED);
        save(session);
    }

    @Override
    public CreateUploadSessionsResponse createSessions(CreateUploadSessionsRequest request, HttpServletRequest httpRequest) {

        List<CreateUploadSessionsResponse.UploadSessionItem> responseList = new ArrayList<>();
        UserSessionObject user = getUserSessionDetails(httpRequest);

        for (CreateUploadSessionsRequest.FileUploadRequest file : request.getFiles()) {

            String uploadId = generateUploadId();
            String publicUploadRef =  generatePublicUploadRef();

            Instant now = Instant.now();
            Instant expiry = now.plusSeconds(file.getExpiresInMinutes() * 60);

            UploadSession session = new UploadSession();

            session.setUploadId(uploadId);
            session.setPublicUploadRef(publicUploadRef);
            session.setUserId(user.getUserID());

            session.setSourceService(request.getSourceService());
            session.setReferenceType("APPLICATION");

            session.setReferenceId(file.getReferenceId());
            session.setCategory(file.getCategory());

            session.setMaxFileSize(file.getMaxFileSize());
            session.setMinFileSize(file.getMinFileSize() != null ? file.getMinFileSize() : 0L);
            session.setMaxChunks(calculateChunks(file.getMaxFileSize()));

            session.setAllowedMimeTypes(convertToJsonFromList(file.getAllowedMime()));
            session.setChunkedUpload(file.getChunkedUpload() != null ? file.getChunkedUpload() : true);

            session.setDocumentId(file.getDocumentId());

            session.setStatus(UPLOAD_STATUS_INITIATED);
            session.setCreatedAt(now);
            session.setExpiresAt(expiry);
            session.setFunctionality(file.getFunctionality());
            session.setTenantId(user.getTenantId());
            session.setMinHeight(file.getMinHeight());
            session.setMaxHeight(file.getMaxHeight());
            session.setMinWidth(file.getMinWidth());
            session.setMaxWidth(file.getMaxWidth());

            save(session);

            String token = fileUploadJWTService.generateUploadToken(session);
            System.out.println(token);
            token = AESEncrypt(token,null);

            responseList.add(
                    new CreateUploadSessionsResponse.UploadSessionItem(
                            publicUploadRef,
                            token,
                            file.getCategory(),
                            file.getExpiresInMinutes(),
                            file.getReferenceId(),
                            session.getMaxChunks().longValue(),
                            getChunkSizeBytes()
                    )
            );
        }

        return new CreateUploadSessionsResponse(responseList);
    }

    public static int calculateChunks(Long fileSizeBytes) {

        long chunkSizeBytes = getChunkSizeBytes();

        return (int) Math.ceil((double) fileSizeBytes / chunkSizeBytes);
    }

    @Transactional
    public CancelResult cancelUpload(String uploadId, HttpServletRequest request) {

        UserSessionObject user = getUserSessionDetails(request);

        UploadSession session = getSession(uploadId,user.getTenantId());

        if (isNull(session)) {
            throw new SPRuntimeError("Invalid uploadId",HttpStatus.BAD_REQUEST);
        }

        if (!user.getUserID().equals(session.getUserId())) {
            throw new SPRuntimeError("Deletion not allowed",HttpStatus.UNAUTHORIZED);
        }

        if (UPLOAD_STATUS_CANCELLED_BY_USER.equals(session.getStatus())) {
            throw new SPRuntimeError("Already cancelled",HttpStatus.CONFLICT);
        }

        if (UPLOAD_STATUS_COMPLETED.equals(session.getStatus())) {
            throw new SPRuntimeError("Cannot cancel completed upload",HttpStatus.CONFLICT);
        }

        session.setStatus(
                UPLOAD_STATUS_CANCELLED_BY_USER
        );

        save(session);

        DocumentMetadata meta = documentMetaDataService.getByUploadId(uploadId, user.getTenantId());

        if (meta != null) {
            meta.setStatus(UPLOAD_STATUS_TEMPORARY);
            documentMetaDataService.save(meta);
        }

        return new CancelResult(
                uploadId,
                UPLOAD_STATUS_CANCELLED_BY_USER,
                "Upload cancelled successfully"
        );
    }

    @Override
    public UploadSession findByPublicUploadRef(String publicUploadRef, String tenantId) {
        return repository.findByPublicUploadRefAndTenantId(publicUploadRef,tenantId).orElse(null);
    }

    @Override
    @Transactional
    public ResponseEntity<?> replaceSession(HttpServletRequest request,String publicUploadRef){
        {

            UserSessionObject user =
                    getUserSessionDetails(request);

            UploadSession oldSession = findByPublicUploadRef(publicUploadRef, user.getTenantId());

            if (!Objects.equals(oldSession.getTenantId(),user.getTenantId()) ||
                    !Objects.equals(user.getUserID(),oldSession.getUserId())) {

                throw new SPRuntimeError("Access Denied",
                        HttpStatus.FORBIDDEN
                );
            }

            if (oldSession.replaceNotAllowed()) {

                throw new SPRuntimeError(
                        "Upload cannot be replaced in current state",
                        HttpStatus.BAD_REQUEST
                );
            }

            Instant now = Instant.now();

            oldSession.setStatus(UPLOAD_STATUS_REPLACED);

            oldSession.setUpdatedAt(now);

            oldSession.setUserId(user.getUserID());

            String newUploadId = generateUploadId();

            String newPublicUploadRef = generatePublicUploadRef();

            UploadSession newSession =   new UploadSession();

            newSession.setUploadId(newUploadId);
            newSession.setPublicUploadRef(newPublicUploadRef );

            newSession.setTenantId(user.getTenantId());
            newSession.setUserId(user.getUserID());
            newSession.setCreatedAt(now);
            newSession.setStatus(UPLOAD_STATUS_INITIATED);

            newSession.setReferenceId(oldSession.getReferenceId());
            newSession.setDocumentId(oldSession.getDocumentId());

            newSession.setCategory(oldSession.getCategory());
            newSession.setSourceService(oldSession.getSourceService());
            newSession.setFunctionality(oldSession.getFunctionality());

            newSession.setPreviousUploadId(oldSession.getUploadId());
            newSession.setMaxChunks(calculateChunks(oldSession.getMaxFileSize()));

            newSession.setReferenceType("APPLICATION");
            newSession.setMaxFileSize(oldSession.getMaxFileSize());
            newSession.setMinFileSize(oldSession.getMinFileSize());

            newSession.setAllowedMimeTypes(oldSession.getAllowedMimeTypes());
            newSession.setChunkedUpload(oldSession.getChunkedUpload());

            newSession.setCreatedAt(now);

            Duration diff = Duration.between(
                                oldSession.getCreatedAt(),
                                oldSession.getExpiresAt()
                           );

            newSession.setExpiresAt(newSession.getCreatedAt().plus(diff));

            oldSession.setReplacedByUploadId(newUploadId);

            String token = fileUploadJWTService.generateUploadToken(newSession);
            token = AESEncrypt(token,null);

            save(oldSession);
            save(newSession);

            DocumentMetadata doc = documentMetaDataService.getByUploadId(oldSession.getUploadId(), user.getTenantId());
            doc.setStatus(UPLOAD_STATUS_REPLACED);
            documentMetaDataService.save(doc);

            ReplaceFileResponse response =  new ReplaceFileResponse();

            response.setOldUploadId(publicUploadRef);
            response.setNewUploadId(newPublicUploadRef);
            response.setUploadToken(token);
            response.setStatus(UPLOAD_STATUS_INITIATED);

            return ResponseEntity.ok(response);
        }
    }

}
