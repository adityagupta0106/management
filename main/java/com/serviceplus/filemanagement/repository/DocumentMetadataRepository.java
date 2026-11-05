package com.serviceplus.filemanagement.repository;

import com.serviceplus.filemanagement.model.persistenceEntity.DocumentMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentMetadataRepository extends JpaRepository<DocumentMetadata, String> {
    Optional<DocumentMetadata> findByUploadIdAndTenantId(String uploadId, String tenantId);
    Optional<DocumentMetadata> findFirstByFileHashAndTenantIdAndStatus(String hash,String tenantId,String status);
}