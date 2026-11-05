package com.serviceplus.filemanagement.repository;

import com.serviceplus.filemanagement.model.persistenceEntity.UploadSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UploadSessionRepository extends JpaRepository<UploadSession, String> {

    Optional<UploadSession> findByUploadIdAndTenantId(String uploadId, String tenantId);

    Optional<UploadSession> findByPublicUploadRefAndTenantId(String publicUploadRef,String tenantId);
}
