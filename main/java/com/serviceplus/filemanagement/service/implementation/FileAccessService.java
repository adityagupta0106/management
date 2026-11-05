package com.serviceplus.filemanagement.service.implementation;

import com.serviceplus.filemanagement.ExceptionHandler.SPRuntimeError;
import com.serviceplus.filemanagement.dto.Base64FileResponse;
import com.serviceplus.filemanagement.dto.FileViewResponse;
import com.serviceplus.filemanagement.dto.SignedUrlResponse;
import com.serviceplus.filemanagement.model.UserSessionObject;
import com.serviceplus.filemanagement.model.persistenceEntity.DocumentMetadata;
import com.serviceplus.filemanagement.model.persistenceEntity.UploadSession;
import com.serviceplus.filemanagement.service.UploadSessionService;
import com.serviceplus.filemanagement.storage.ObjectStorage;
import io.minio.http.Method;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.serviceplus.filemanagement.Utility.Utility.getUserSessionDetails;
import static com.serviceplus.filemanagement.Utility.Utility.resolveBaseUrl;
import static java.util.Objects.isNull;

@Service
public class FileAccessService {

    @Autowired
    private ObjectStorage storageService;

    @Autowired
    private DocumentMetaDataService metaService;

    @Autowired
    private UploadSessionService uploadSessionService;

    @Value("${file.preview.access.duration.seconds}")
    private Integer EXPIRY_SECONDS;

    public SignedUrlResponse getSignedUrlByUploadId(
            String uploadId,
            boolean download,
            HttpServletRequest request
    ) {
        UserSessionObject user =getUserSessionDetails(request);

        UploadSession session = uploadSessionService.findByPublicUploadRef(uploadId, user.getTenantId());

        if(session.isViewNotAllowed()){
            return new SignedUrlResponse("", 0,"");
        }

        DocumentMetadata doc = metaService.getByUploadId(session.getUploadId(),user.getTenantId());

        validateAccess(doc, user,uploadId);

        Map<String, String> params = new HashMap<>();

        if (download) {
            params.put("response-content-disposition",
                    "attachment; filename=\"" + doc.getOriginalFileName() + "\"");
        } else {
            params.put("response-content-disposition",
                    "inline; filename=\"" + doc.getOriginalFileName() + "\"");
        }

        params.put("response-content-type", doc.getMimeType());

        String gatewayEndpoint = resolveBaseUrl(request);

        String url = storageService.getPresignedUrl(
                gatewayEndpoint,
                doc.getBucketName(),
                doc.getObjectKey(),
                doc.getOriginalFileName(),
                doc.getMimeType(),
                download,
                EXPIRY_SECONDS,
                params,
                Method.GET
        );

        return new SignedUrlResponse(url, EXPIRY_SECONDS,session.getPublicUploadRef());
    }

    public SignedUrlResponse getThumbnail(String uploadId, HttpServletRequest request) {

        UserSessionObject user = getUserSessionDetails(request);
        UploadSession session = uploadSessionService.findByPublicUploadRef(uploadId, user.getTenantId());

        if(session.isViewNotAllowed()){
            return new SignedUrlResponse("", 0,"");
        }

        DocumentMetadata doc = metaService.getByUploadId(session.getUploadId(),user.getTenantId());

        validateAccess(doc, user,uploadId);

        if (doc.getThumbnailKey() == null) {
            return new SignedUrlResponse("NA", EXPIRY_SECONDS,"");
        }

        Map<String, String> params = new HashMap<>();

        params.put("response-content-disposition",
                    "inline; filename=\"" + "preview.jpg" + "\"");

        params.put("response-content-type", "image/jpeg");

        String gatewayEndpoint = resolveBaseUrl(request);

        String url = storageService.getPresignedUrl(
                gatewayEndpoint,
                doc.getBucketName(),
                doc.getThumbnailKey(),
                "preview.jpg",
                "image/jpeg",
                false,
                EXPIRY_SECONDS,
                params,
                Method.GET
        );

        return new SignedUrlResponse(url, EXPIRY_SECONDS,session.getPublicUploadRef());
    }

    public FileViewResponse getFileView(List<String> uploadId, HttpServletRequest request) {

        Map<String, FileViewResponse.Data> resultMap = new HashMap<>();

        uploadId.forEach(id -> {

            List<FileViewResponse.Data.TypedSignedUrl> items = new ArrayList<>();

            SignedUrlResponse fullFile = getSignedUrlByUploadId(id, false, request);

            items.add(new FileViewResponse.Data.TypedSignedUrl(
                    "preview",fullFile
            ));

            SignedUrlResponse thumbNail = getThumbnail(id,request);

            items.add(new FileViewResponse.Data.TypedSignedUrl(
                    "thumbnail",thumbNail
            ));

            SignedUrlResponse download = getSignedUrlByUploadId(id, true, request);

            items.add(new FileViewResponse.Data.TypedSignedUrl(
                    "download",  download
            ));

            FileViewResponse.Data data = new FileViewResponse.Data();
            data.setData(items);

            resultMap.put(id, data);
        });

        FileViewResponse response = new FileViewResponse();
        response.setResults(resultMap);

        return response;
    }

    private void validateAccess(DocumentMetadata doc, UserSessionObject user,String uploadId) {

        if(isNull(doc)){
            throw new SPRuntimeError("Invalid UploadId " + uploadId,HttpStatus.BAD_REQUEST);
        }

        if (!doc.getUserId().equals(user.getUserID())) {
            //throw new SPRuntimeError("Unauthorized access",HttpStatus.UNAUTHORIZED);
        }
    }

    public Base64FileResponse getFileAsBase64(String uploadId, HttpServletRequest request) {

        UserSessionObject user = getUserSessionDetails(request);

        if (isNull(user)) {
            throw new SPRuntimeError("Unauthorized Request", HttpStatus.UNAUTHORIZED);
        }

        UploadSession session = uploadSessionService.findByPublicUploadRef(uploadId, user.getTenantId());

        if (isNull(session)) {
            throw new SPRuntimeError("Invalid Upload Reference", HttpStatus.BAD_REQUEST);
        }

        if (session.isViewNotAllowed()) {
            throw new SPRuntimeError("File view is not allowed", HttpStatus.FORBIDDEN);
        }

        DocumentMetadata doc = metaService.getByUploadId(session.getUploadId(), user.getTenantId());

        validateAccess(doc, user, uploadId);

        if (isNull(doc.getBucketName()) || isNull(doc.getObjectKey())) {
            throw new SPRuntimeError("File object not found", HttpStatus.NOT_FOUND);
        }

        try {

            byte[] fileBytes = storageService.getObjectBytes(doc.getBucketName(), doc.getObjectKey());

            String base64 = Base64.getEncoder().encodeToString(fileBytes);

            return new Base64FileResponse(doc.getOriginalFileName(), doc.getMimeType(), base64);

        } catch (Exception e) {

            throw new SPRuntimeError("Failed to read file", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
