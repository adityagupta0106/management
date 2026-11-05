package com.serviceplus.filemanagement.service.implementation;

import com.serviceplus.filemanagement.ExceptionHandler.SPRuntimeError;
import com.serviceplus.filemanagement.Utility.FileUploadJWTService;
import com.serviceplus.filemanagement.dto.UploadStatusResponse;
import com.serviceplus.filemanagement.model.persistenceEntity.UploadSession;
import com.serviceplus.filemanagement.model.UploadToken;
import com.serviceplus.filemanagement.model.UserSessionObject;
import com.serviceplus.filemanagement.service.ChunkService;
import com.serviceplus.filemanagement.service.IChunkMergeService;
import com.serviceplus.filemanagement.service.UploadService;
import com.serviceplus.filemanagement.service.UploadSessionService;
import com.serviceplus.filemanagement.storage.ObjectStorage;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.List;

import static com.serviceplus.filemanagement.Utility.ApplicationConstants.TEMP_BUCKET;
import static com.serviceplus.filemanagement.Utility.ApplicationConstants.TEMP_CHUNK_UPLOAD_PREFIX;
import static com.serviceplus.filemanagement.Utility.Utility.*;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

@Service
public class TokenBasedUpload implements UploadService {

    @Autowired
    private ChunkService chunkService;

    @Autowired
    private FileUploadJWTService jwtService;

    @Autowired
    private ObjectStorage objectStorage;

    @Autowired
    private UploadSessionService uploadSessionService;

    @Autowired
    private IChunkMergeService chunkMergeService;

    @Autowired
    private FileAccessService fileAccessService;

    private static final Logger logger = LoggerFactory.getLogger(TokenBasedUpload.class);

    @Override
    public UploadStatusResponse uploadChunk(
            HttpServletRequest request,
            String uploadId,
            int chunkNumber,
            InputStream data,
            int totalChunks
    ) {

        logger.info("Chunk upload request received uploadId={}, chunkNumber={}, totalChunks={}",uploadId,chunkNumber,totalChunks);

        String token = getBearerToken(request);

        if(isNull(token)) {
            logger.warn("Unauthorized upload request for uploadId={}", uploadId);
        }

        if (isEmpty(token)) {
            throw new SPRuntimeError("Unauthorized Request [01]", HttpStatus.UNAUTHORIZED);
        }

        if (token.contains("%2B")) {
            token = token.replace("%2B", "+");
        }
        if (token.contains(" ")) {
            token = token.replaceAll(" ", "+");
        }
        token = AESDecrypt(token, null);

        UploadToken payload = jwtService.validateUploadToken(token);
        UserSessionObject user = getUserSessionDetails(request);
        boolean completed = false;

        validateUpload(payload, uploadId, chunkNumber, totalChunks,user.getTenantId(),user);

        logger.info("Upload token validated uploadId={}, tenantId={}, userId={}",payload.getUploadId(),payload.getTenantId(),payload.getUserId());

        UploadSession session = null;
        if (uploadSessionService != null) {
            session = validateUploadSession(payload,uploadId);
        }

        logger.info("Upload session validated uploadId={}",uploadId);

        BufferedInputStream bufferedStream =  new BufferedInputStream(data);

        if (chunkNumber == 0) {
//            String originalFileName = request.getHeader("X-File-Name");
//
//            String mimeType = detectMimeType(bufferedStream,originalFileName);
//
//            logger.info("Detected mimeType={} for uploadId={}",mimeType,uploadId);
//
//            validateMimeType(
//                    mimeType,
//                    payload.getAllowedMime(),
//                    payload.getUploadId()
//            );
        }

        if (chunkService.isChunkAlreadyUploaded(uploadId, chunkNumber)
                && !chunkService.allChunksReceived(uploadId, totalChunks)) {

            logger.warn("Duplicate chunk upload ignored uploadId={}, chunkNumber={}",uploadId,chunkNumber);

            return buildResponse(uploadId,payload, completed,totalChunks,request);
        }

        String objectKey = getChunkObjectKey(uploadId, chunkNumber);

        logger.info("Uploading chunk uploadId={}, chunkNumber={}, objectKey={}",uploadId,chunkNumber,objectKey);

        ensureBucketExists(TEMP_BUCKET);
        long chunkSize = objectStorage.store(TEMP_BUCKET, objectKey, bufferedStream);

        trackUploadedBytes(uploadId,chunkSize, payload.getMaxFileSize(),TEMP_BUCKET, objectKey,payload);

        chunkService.markChunkReceived(uploadId, chunkNumber);

        if (session != null) {
            uploadSessionService.markInProgress(uploadId,user.getTenantId());
        }

        if (chunkService.allChunksReceived(uploadId, totalChunks)) {
             String bucket = createBucket(user.getTenantId(), payload.getSourceService(),payload.getFunctionality());
             ensureBucketExists(bucket);
             String obKey = payload.getPersistentUploadId().concat("-").concat("full");
             chunkMergeService.mergeToFinalObject(request,bucket,obKey,uploadId,totalChunks,user,payload);
             completed = true;
        }

        logger.info("Chunk stored uploadId={}, chunkNumber={}, chunkSize={} bytes",uploadId,chunkNumber,chunkSize);

        return buildResponse(uploadId, payload,completed,totalChunks,request);
    }

    private void validateUpload(
            UploadToken payload,
            String uploadId,
            int chunkNumber,
            int totalChunks,
            String tenantId, UserSessionObject user) {


        logger.info("Validating upload id {} for req tenantId {} req userId {} , logged in userid {} ,logged in tenantId {}"
                    ,payload.getUploadId(),payload.getTenantId(),payload.getUserId(),user.getUserID(),tenantId);

        if (!tenantId.equals(payload.getTenantId()) || !payload.getUserId().equals(user.getUserID())) {
            logger.warn("Upload authorization failed: uploadId={}, requestTenantId={}, tokenTenantId={}, " + "tokenUserId={}, loggedInUserId={}", uploadId, tenantId, payload.getTenantId(), payload.getUserId(), user.getUserID());
            throw new SPRuntimeError("You are not authorized to upload this file.", HttpStatus.UNAUTHORIZED);
        }

        if (!uploadId.equals(payload.getUploadId())) {
            logger.warn("Upload ID validation failed: requestUploadId={}, tokenUploadId={}", uploadId, payload.getUploadId());
            throw new SPRuntimeError("The upload session is invalid or has expired. Please start the upload again.", HttpStatus.BAD_REQUEST);
        }

        if (totalChunks > payload.getMaxChunks()) {
            logger.warn("Chunk limit exceeded: uploadId={}, requestedChunks={}, allowedChunks={}", uploadId, totalChunks, payload.getMaxChunks());
            throw new SPRuntimeError("The file is too large to upload. Please select a smaller file.", HttpStatus.PAYLOAD_TOO_LARGE);
        }

        if (chunkNumber < 0 || chunkNumber >= payload.getMaxChunks()) {
            logger.warn("Invalid chunk number: uploadId={}, chunkNumber={}, maxChunks={}", uploadId, chunkNumber, payload.getMaxChunks());
            throw new SPRuntimeError("The uploaded file contains an invalid chunk. Please try the upload again.", HttpStatus.BAD_REQUEST);
        }
    }

    private UploadSession validateUploadSession(UploadToken payload, String uploadId) {

        UploadSession session = uploadSessionService.findByPublicUploadRef(uploadId,payload.getTenantId());

        if (!session.getUserId().equals(payload.getUserId())) {
            throw new SPRuntimeError("User mismatch", HttpStatus.UNAUTHORIZED);
        }

        if (session.isExpired()) {
            throw new SPRuntimeError("Upload session expired", HttpStatus.GONE);
        }

        if (session.isCompleted()) {
            throw new SPRuntimeError("Upload already completed", HttpStatus.CONFLICT);
        }

        return session;
    }

    private void trackUploadedBytes(String uploadId, long chunkSize, long maxSize, String tempBucket, String objectKey, UploadToken payload) {

        if (getChunkSizeBytes() < chunkSize) {
            throw new SPRuntimeError("Chunk file size exceeded", HttpStatus.PAYLOAD_TOO_LARGE);
        }

        Long total = chunkService.uploadedBytes(uploadId, chunkSize);

        if (total != null && total > maxSize) {
            throw new SPRuntimeError("File size exceeded", HttpStatus.PAYLOAD_TOO_LARGE);
        }
    }

    private String getChunkObjectKey(String uploadId, int chunkNumber) {
        return TEMP_CHUNK_UPLOAD_PREFIX + "/" + uploadId + "/chunk-" + chunkNumber;
    }

    private UploadStatusResponse buildResponse(String uploadId, UploadToken payload, boolean completed, int totalChunks, HttpServletRequest request) {

        UploadStatusResponse res = new UploadStatusResponse();

        long uploadedChunks = completed ? totalChunks : chunkService.getChunkCount(uploadId);

        int percent = (int) ((uploadedChunks * 100.0) / totalChunks);

        res.setUploadId(uploadId);
        res.setUploadedChunks(uploadedChunks);
        res.setTotalChunks(totalChunks);
        res.setMaxFileSize(payload.getMaxFileSize());
        res.setProgressPercent(percent);
        res.setCompleted(uploadedChunks == totalChunks);
        res.setStatus(uploadedChunks == totalChunks ? "COMPLETED" : "IN_PROGRESS");

        if(res.getCompleted()){
            res.setThumbnailURL(
                    fileAccessService.getThumbnail(uploadId,request).getUrl()
                    );
            res.setPreviewUrl(
                    fileAccessService.getSignedUrlByUploadId(uploadId,false,request).getUrl()
            );
        }

        return res;
    }

    private void validateMimeType(String detectedMime, List<String> allowedMime, String uploadId) {

        logger.info("MIME validation started: uploadId={}, detectedMime={}, allowedMimeCount={}", uploadId, detectedMime, allowedMime != null ? allowedMime.size() : 0);

        if (detectedMime == null) {
            logger.warn("MIME detection failed: uploadId={}", uploadId);
            throw new SPRuntimeError("We could not determine the file type. Please upload a valid file.", HttpStatus.BAD_REQUEST);
        }

        boolean allowed = requireNonNull(allowedMime).stream().anyMatch(m -> m.equalsIgnoreCase(detectedMime));

        if (!allowed) {

            logger.warn("MIME type rejected: uploadId={}, detectedMime={}, allowedMimeTypes={}", uploadId, detectedMime, allowedMime);
            throw new SPRuntimeError("The selected file type is not allowed for this upload.", HttpStatus.BAD_REQUEST);
        }
    }

}


