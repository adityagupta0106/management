package com.serviceplus.filemanagement.service.implementation;

import static com.serviceplus.filemanagement.Utility.ApplicationConstants.UPLOAD_STATUS_COMPLETED;
import static com.serviceplus.filemanagement.Utility.ApplicationConstants.UPLOAD_STATUS_UPLOADED;
import static com.serviceplus.filemanagement.Utility.Utility.AESDecrypt;
import static com.serviceplus.filemanagement.Utility.Utility.generatePublicUploadRef;
import static com.serviceplus.filemanagement.Utility.Utility.generateUploadId;
import static com.serviceplus.filemanagement.Utility.Utility.getUserSessionDetails;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.serviceplus.filemanagement.ExceptionHandler.SPRuntimeError;
import com.serviceplus.filemanagement.dto.DSCSignRequest;
import com.serviceplus.filemanagement.dto.DSCSignResponse;
import com.serviceplus.filemanagement.dto.DSCVerificationDetails;
import com.serviceplus.filemanagement.dto.DSCVerificationRequest;
import com.serviceplus.filemanagement.feign.UserManagementClient;
import com.serviceplus.filemanagement.model.UserSessionObject;
import com.serviceplus.filemanagement.model.persistenceEntity.DocumentMetadata;
import com.serviceplus.filemanagement.model.persistenceEntity.UploadSession;
import com.serviceplus.filemanagement.repository.UploadSessionRepository;
import com.serviceplus.filemanagement.storage.ObjectStorage;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class DSCManagementService {

	@Autowired
	private UploadSessionRepository uploadSessionRepository;

	@Autowired
	private DocumentMetaDataService documentMetaDataService;

	@Autowired
	private ObjectStorage objectStorage;

	@Autowired
	private UserManagementClient userManagementClient;

	@Autowired
	private DSCVerificationService dscVerificationService;
	
	@Autowired
    private FileAccessService fileAccessService;

	@Transactional
	public DSCSignResponse signDocument(HttpServletRequest httpRequest, DSCSignRequest request) {

		validateRequest(request);

		
		String publicUploadRef = request.getFileId();

		UploadSession oldSession = uploadSessionRepository
				.findByPublicUploadRefAndTenantId(publicUploadRef, getTenantFromRequest(httpRequest,request)).orElse(null);

		
		if (oldSession == null) {

			throw new SPRuntimeError("Invalid file ID", HttpStatus.BAD_REQUEST);
		}

		
		if (!request.getUserId().equals(oldSession.getUserId())) {

			throw new SPRuntimeError("User is not authorized for this document", HttpStatus.FORBIDDEN);
		}

		
		DocumentMetadata originalDocument = documentMetaDataService.getByUploadId(oldSession.getUploadId(),
				oldSession.getTenantId());

		if (originalDocument == null) {

			throw new SPRuntimeError("Document not found", HttpStatus.NOT_FOUND);
		}

//		/*
//		 * Original document must be uploaded.
//		 */
//		if (!UPLOAD_STATUS_UPLOADED.equals(originalDocument.getStatus())) {
//
//			throw new SPRuntimeError("Document is not available for signing", HttpStatus.BAD_REQUEST);
//		}

		/*
		 * Get approved DSC from UserManagement.
		 */
		request.setDocumentType(getDocumentType(originalDocument.getMimeType()));
		byte[] signedDocument = Base64.getDecoder().decode(request.getSignedBase64());
//		ResponseEntity<DSCVerificationDetails> dscResponse = userManagementClient.
//				getDSCVerificationDetails(new DSCVerificationRequest(request.getUserId()));
//
//		if (dscResponse == null || dscResponse.getBody() == null) {
//
//			throw new SPRuntimeError("Unable to retrieve DSC details", HttpStatus.BAD_GATEWAY);
//		}
//
//		DSCVerificationDetails dsc = dscResponse.getBody();
//		
//		if (dsc.getCertificate() == null || dsc.getCertificate().isBlank()) {
//
//			throw new SPRuntimeError("DSC certificate not found", HttpStatus.BAD_REQUEST);
//		}
//
//		/*
//		 * Verify the signed content.
//		 *
//		 */
//		
//		byte[] signedDocument = dscVerificationService.verifySignedDocument(request.getSignedBase64(), request.getKey(),
//				dsc.getCertificate(), request.getDocumentType());
//
//		if (signedDocument == null || signedDocument.length == 0) {
//
//			throw new SPRuntimeError("Signed document is empty", HttpStatus.BAD_REQUEST);
//		}

		UploadSession signedSession = createSignedSession(oldSession);

		/*
		 * Store signed document in MinIO.
		 */
		String signedObjectKey = createSignedObjectKey(signedSession, originalDocument);

		long signedSize;

		try (InputStream inputStream = new ByteArrayInputStream(signedDocument)) {

			signedSize = objectStorage.store(originalDocument.getBucketName(), signedObjectKey, inputStream);

		} catch (Exception ex) {

			ex.printStackTrace();

			throw new SPRuntimeError("Unable to store signed document", HttpStatus.INTERNAL_SERVER_ERROR);
		}

		/*
		 * Create metadata for signed document.
		 */
		DocumentMetadata signedMetadata = createSignedMetadata(originalDocument, signedSession, signedObjectKey,
				signedSize, signedDocument);

		
		uploadSessionRepository.save(signedSession);

		documentMetaDataService.save(signedMetadata);

		
		DSCSignResponse response = new DSCSignResponse();

		response.setSuccess(true);
		response.setMessage("Document signed successfully");

		response.setFileId(signedSession.getPublicUploadRef());
		
		response.setPreviewUrl(fileAccessService.getSignedUrlByUploadId(signedSession.getPublicUploadRef(),false,httpRequest).getUrl());

		return response;
	}
	
	private String getDocumentType(String mimeType) {

	    if (mimeType == null || mimeType.isBlank()) {
	        throw new SPRuntimeError(
	                "Document MIME type is not available",
	                HttpStatus.BAD_REQUEST);
	    }

	    if ("application/pdf".equalsIgnoreCase(mimeType)) {
	        return "pdf";
	    }

	    if ("application/xml".equalsIgnoreCase(mimeType)
	            || "text/xml".equalsIgnoreCase(mimeType)) {
	        return "xml";
	    }

	    throw new SPRuntimeError(
	            "Unsupported document type: " + mimeType,
	            HttpStatus.BAD_REQUEST);
	}

	private UploadSession createSignedSession(UploadSession oldSession) {

		Instant now = Instant.now();

		String newUploadId = generateUploadId();

		String newPublicUploadRef = generatePublicUploadRef();

		UploadSession newSession = new UploadSession();

		newSession.setUploadId(newUploadId);

		newSession.setPublicUploadRef(newPublicUploadRef);

		newSession.setTenantId(oldSession.getTenantId());

		newSession.setUserId(oldSession.getUserId());

		newSession.setReferenceId(oldSession.getReferenceId());

		newSession.setReferenceType(oldSession.getReferenceType());

		newSession.setDocumentId(oldSession.getDocumentId());

		newSession.setCategory(oldSession.getCategory());

		newSession.setSourceService(oldSession.getSourceService());

		newSession.setFunctionality(oldSession.getFunctionality());

		newSession.setAllowedMimeTypes(oldSession.getAllowedMimeTypes());

		newSession.setChunkedUpload(oldSession.getChunkedUpload());

		newSession.setMaxFileSize(oldSession.getMaxFileSize());

		newSession.setMinFileSize(oldSession.getMinFileSize());

		newSession.setMaxChunks(oldSession.getMaxChunks());

		newSession.setPreviousUploadId(oldSession.getUploadId());

		newSession.setStatus(UPLOAD_STATUS_UPLOADED);

		newSession.setCreatedAt(now);

		if (oldSession.getExpiresAt() != null && oldSession.getCreatedAt() != null) {

			Duration duration = Duration.between(oldSession.getCreatedAt(), oldSession.getExpiresAt());

			newSession.setExpiresAt(now.plus(duration));

		} else {

			newSession.setExpiresAt(now.plus(Duration.ofMinutes(30)));
		}

		return newSession;
	}

	private DocumentMetadata createSignedMetadata(DocumentMetadata original, UploadSession signedSession,
			String objectKey, long size, byte[] signedDocument) {

		DocumentMetadata metadata = new DocumentMetadata();

		metadata.setUploadId(signedSession.getUploadId());

		metadata.setDocumentId(original.getDocumentId());

		metadata.setUserId(original.getUserId());

		metadata.setReferenceType(original.getReferenceType());

		metadata.setReferenceId(original.getReferenceId());

		metadata.setOriginalFileName(original.getOriginalFileName());

		metadata.setFileName(original.getFileName());

		metadata.setCategory(original.getCategory());

		metadata.setBucketName(original.getBucketName());

		metadata.setObjectKey(objectKey);

		metadata.setMimeType(original.getMimeType());

		metadata.setSizeBytes(size);

		metadata.setFileHash(calculateHash(signedDocument));

		metadata.setStatus(UPLOAD_STATUS_UPLOADED);

		metadata.setCreatedAt(Instant.now());

		metadata.setUpdatedAt(Instant.now());

		metadata.setTenantId(original.getTenantId());

		metadata.setThumbnailKey(null);

		return metadata;
	}

	private String createSignedObjectKey(UploadSession signedSession, DocumentMetadata original) {

		String fileName = original.getFileName();

		if (fileName == null || fileName.isBlank()) {

			fileName = "signed-document";
		}

		return "signed"+File.separator + signedSession.getUploadId() + File.separator + fileName;
	}

	private String calculateHash(byte[] data) {

		try {

			MessageDigest digest = MessageDigest.getInstance("SHA-256");

			byte[] hash = digest.digest(data);

			StringBuilder builder = new StringBuilder();

			for (byte b : hash) {

				builder.append(String.format("%02x", b));
			}

			return builder.toString();

		} catch (Exception ex) {

			throw new SPRuntimeError("Hash generation failed", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private void validateRequest(DSCSignRequest request) {

		if (request == null) {

			throw new SPRuntimeError("Invalid request", HttpStatus.BAD_REQUEST);
		}

		if (request.getFileId() == null || request.getFileId().isBlank()) {

			throw new SPRuntimeError("File ID is required", HttpStatus.BAD_REQUEST);
		}

		if (request.getUserId() == null) {

			throw new SPRuntimeError("User ID is required", HttpStatus.BAD_REQUEST);
		}

		if (request.getSignedBase64() == null || request.getSignedBase64().isBlank()) {

			throw new SPRuntimeError("Signed document is required", HttpStatus.BAD_REQUEST);
		}

		if (request.getKey() == null || request.getKey().isBlank()) {

			throw new SPRuntimeError("Signing key is required", HttpStatus.BAD_REQUEST);
		}

	}

	private String getTenantFromRequest(HttpServletRequest httpRequest, DSCSignRequest request) {

		UserSessionObject user =getUserSessionDetails(httpRequest);
		if(user==null) {
			throw new UnsupportedOperationException("Use trusted tenantId from Processing");
		}
		return user.getTenantId();
	}
}
