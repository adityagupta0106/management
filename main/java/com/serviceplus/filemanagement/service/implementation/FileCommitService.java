package com.serviceplus.filemanagement.service.implementation;

import com.serviceplus.filemanagement.dto.CommitResponse;
import com.serviceplus.filemanagement.dto.SignedUrlResponse;
import com.serviceplus.filemanagement.model.UserSessionObject;
import com.serviceplus.filemanagement.model.persistenceEntity.DocumentMetadata;
import com.serviceplus.filemanagement.model.persistenceEntity.UploadSession;
import com.serviceplus.filemanagement.service.UploadSessionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.serviceplus.filemanagement.Utility.ApplicationConstants.*;
import static com.serviceplus.filemanagement.Utility.Utility.getUserSessionDetails;

@Service
public class FileCommitService {

    @Autowired
    private DocumentMetaDataService metaService;

    @Autowired
    private UploadSessionService uploadSessionService;

    public List<CommitResponse.CommitResult> commitBatch(List<String> uploadIds, HttpServletRequest servletRequest) {

        List<CommitResponse.CommitResult> results = new ArrayList<>();
        UserSessionObject user = getUserSessionDetails(servletRequest);

        for (String uploadId : uploadIds) {

            try {
                results.add(process(uploadId,user));
            } catch (Exception e) {
                e.printStackTrace();
                results.add(new CommitResponse.CommitResult(
                        uploadId,
                        UPLOAD_STATUS_FAILED,
                        "Something went wrong"
                ));
            }
        }

        return results;
    }

    private CommitResponse.CommitResult process(String uploadId, UserSessionObject user) {

        UploadSession session = uploadSessionService.findByPublicUploadRef(uploadId, user.getTenantId());

        if(session.isViewNotAllowed()){
            return new CommitResponse.CommitResult(uploadId, "FAILED", "Invalid uploadId");
        }

        DocumentMetadata doc = metaService.getByUploadId(session.getUploadId(),user.getTenantId());

        if (doc == null) {
            return new CommitResponse.CommitResult(uploadId, "FAILED", "Invalid uploadId");
        }

        if(!doc.getUserId().equals(user.getUserID())){
            return new CommitResponse.CommitResult(uploadId, "FAILED", "Unauthorised user");
        }

        String currentStatus = doc.getStatus();

        if (UPLOAD_STATUS_UPLOADED.equals(currentStatus)) {
            return new CommitResponse.CommitResult(uploadId, "UPLOADED", "Already committed");
        }

        if (!UPLOAD_STATUS_TEMPORARY.equals(currentStatus)) {
            return new CommitResponse.CommitResult(
                    uploadId,
                    "FAILED",
                    "Invalid state transition from " + currentStatus
            );
        }

        uploadSessionService.markCompleted(uploadId,user.getTenantId());

        doc.setStatus(UPLOAD_STATUS_UPLOADED);
        doc.setUpdatedAt(Instant.now());
        metaService.save(doc);

        return new CommitResponse.CommitResult(uploadId, "UPLOADED", "Success");
    }

}
