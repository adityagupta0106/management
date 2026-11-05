package com.serviceplus.filemanagement.model.persistenceEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

import static com.serviceplus.filemanagement.Utility.ApplicationConstants.*;

@Entity
@Table(name = "upload_session",schema = DB_SCHEMA)
public class UploadSession {

    @Id
    @Column(name = "upload_id")
    private String uploadId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "reference_id", nullable = false)
    private String referenceId;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "max_file_size")
    private Long maxFileSize;

    @Column(name = "min_file_size")
    private Long minFileSize;

    @Column(name = "max_chunks")
    private Integer maxChunks;

    @Column(name = "status", nullable = false)
    private String status; // INITIATED, IN_PROGRESS, COMPLETED, EXPIRED

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "source_service")
    private String sourceService;

    @Column(name = "reference_type")
    private String referenceType;

    @Column(name = "allowed_mime_types")
    private String allowedMimeTypes;

    @Column(name = "chunked_upload")
    private Boolean chunkedUpload;

    @Column(name = "document_id")
    private String documentId;

    @Column(name = "functionality")
    private String functionality;

    @Column(name = "previous_upload_id")
    private String previousUploadId;

    @Column(name = "public_upload_ref")
    private String publicUploadRef;

    @Column(name = "replaced_by_upload_id")
    private String replacedByUploadId;

    @Column(name = "tenant_id")
    private String tenantId;

    transient private Integer minWidth;
    transient private Integer maxHeight;
    transient private Integer maxWidth;
    transient private Integer minHeight;

    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }

    public boolean isCompleted() {
        return UPLOAD_STATUS_COMPLETED.equalsIgnoreCase(this.status);
    }

    public boolean replaceNotAllowed(){
        return UPLOAD_STATUS_DELETE_PENDING.equals(this.status) || UPLOAD_STATUS_DELETED.equals(this.status)
                || UPLOAD_STATUS_REPLACED.equals(this.status);
    }

    public boolean isViewNotAllowed(){
        return UPLOAD_STATUS_DELETE_PENDING.equals(this.status) || UPLOAD_STATUS_DELETED.equals(this.status)
                || UPLOAD_STATUS_REPLACED.equals(this.status);
    }

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getMaxChunks() {
        return maxChunks;
    }

    public void setMaxChunks(Integer maxChunks) {
        this.maxChunks = maxChunks;
    }

    public Long getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(Long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getSourceService() {
        return sourceService;
    }

    public void setSourceService(String sourceService) {
        this.sourceService = sourceService;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public String getAllowedMimeTypes() {
        return allowedMimeTypes;
    }

    public void setAllowedMimeTypes(String allowedMimeTypes) {
        this.allowedMimeTypes = allowedMimeTypes;
    }

    public Boolean getChunkedUpload() {
        return chunkedUpload;
    }

    public void setChunkedUpload(Boolean chunkedUpload) {
        this.chunkedUpload = chunkedUpload;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public Long getMinFileSize() {
        return minFileSize;
    }

    public void setMinFileSize(Long minFileSize) {
        this.minFileSize = minFileSize;
    }

    public String getFunctionality() {
        return functionality;
    }

    public void setFunctionality(String functionality) {
        this.functionality = functionality;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getPreviousUploadId() {
        return previousUploadId;
    }

    public void setPreviousUploadId(String previousUploadId) {
        this.previousUploadId = previousUploadId;
    }

    public String getPublicUploadRef() {
        return publicUploadRef;
    }

    public void setPublicUploadRef(String publicUploadRef) {
        this.publicUploadRef = publicUploadRef;
    }

    public String getReplacedByUploadId() {
        return replacedByUploadId;
    }

    public void setReplacedByUploadId(String replacedByUploadId) {
        this.replacedByUploadId = replacedByUploadId;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getMinWidth() {
        return minWidth;
    }

    public void setMinWidth(Integer minWidth) {
        this.minWidth = minWidth;
    }

    public Integer getMaxHeight() {
        return maxHeight;
    }

    public void setMaxHeight(Integer maxHeight) {
        this.maxHeight = maxHeight;
    }

    public Integer getMaxWidth() {
        return maxWidth;
    }

    public void setMaxWidth(Integer maxWidth) {
        this.maxWidth = maxWidth;
    }

    public Integer getMinHeight() {
        return minHeight;
    }

    public void setMinHeight(Integer minHeight) {
        this.minHeight = minHeight;
    }
}
