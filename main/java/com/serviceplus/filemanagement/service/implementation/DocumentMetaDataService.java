package com.serviceplus.filemanagement.service.implementation;

import com.serviceplus.filemanagement.model.persistenceEntity.DocumentMetadata;
import com.serviceplus.filemanagement.model.UploadToken;
import com.serviceplus.filemanagement.repository.DocumentMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

import static com.serviceplus.filemanagement.Utility.ApplicationConstants.UPLOAD_STATUS_TEMPORARY;
import static com.serviceplus.filemanagement.Utility.ApplicationConstants.UPLOAD_STATUS_UPLOADED;

@Service
public class DocumentMetaDataService {

    @Autowired
    private DocumentMetadataRepository repository;

    public void save(DocumentMetadata document) {
        repository.saveAndFlush(document);
    }

    public void createTempDocument(UploadToken payload, String bucket, String objectKey, String mimeType, String fileSignature, String thumbnailKey) {

        DocumentMetadata doc = new DocumentMetadata();

        doc.setDocumentId(payload.getDocumentId());

        doc.setUploadId(payload.getPersistentUploadId());
        doc.setUserId(payload.getUserId());

        doc.setReferenceType(payload.getReferenceType());
        doc.setReferenceId(payload.getReferenceId());
        doc.setCategory(payload.getCategory());

        doc.setOriginalFileName(payload.getFileName());
        doc.setFileName(payload.getFileName());
        doc.setMimeType(mimeType);

        doc.setBucketName(bucket);
        doc.setObjectKey(objectKey);
        doc.setThumbnailKey(thumbnailKey);
        doc.setSizeBytes(payload.getMaxFileSize());

        doc.setFileHash(fileSignature);
        doc.setStatus(UPLOAD_STATUS_TEMPORARY);

        Instant now = Instant.now();
        doc.setCreatedAt(now);
        doc.setUpdatedAt(now);
        doc.setTenantId(payload.getTenantId());

        save(doc);
    }

    public DocumentMetadata getByUploadId(String uploadId,String tenantId) {
        return repository.findByUploadIdAndTenantId(uploadId,tenantId).orElse(null);
    }

    public Optional<DocumentMetadata> findByFileHash(String fileSignature,String tenantId){
        return repository.findFirstByFileHashAndTenantIdAndStatus(fileSignature,tenantId,UPLOAD_STATUS_UPLOADED);
    }
}
