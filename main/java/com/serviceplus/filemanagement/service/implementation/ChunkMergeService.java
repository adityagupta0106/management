package com.serviceplus.filemanagement.service.implementation;

import com.serviceplus.filemanagement.ExceptionHandler.SPRuntimeError;
import com.serviceplus.filemanagement.Utility.FileUploadJWTService;
import com.serviceplus.filemanagement.fileProcessor.FileProcessor;
import com.serviceplus.filemanagement.fileProcessor.FileProcessorFactory;
import com.serviceplus.filemanagement.model.UploadToken;
import com.serviceplus.filemanagement.model.UserSessionObject;
import com.serviceplus.filemanagement.model.persistenceEntity.DocumentMetadata;
import com.serviceplus.filemanagement.service.ChunkService;
import com.serviceplus.filemanagement.service.IChunkMergeService;
import com.serviceplus.filemanagement.service.UploadSessionService;
import com.serviceplus.filemanagement.storage.ObjectStorage;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.serviceplus.filemanagement.Utility.ApplicationConstants.TEMP_BUCKET;
import static com.serviceplus.filemanagement.Utility.ApplicationConstants.TEMP_CHUNK_UPLOAD_PREFIX;
import static com.serviceplus.filemanagement.Utility.FileTypeUtil.resolveExtension;
import static com.serviceplus.filemanagement.Utility.Utility.*;
import static java.util.Objects.isNull;

@Service
public class ChunkMergeService implements IChunkMergeService {

    @Autowired
    private ObjectStorage objectStorage;

    @Autowired
    private FileUploadJWTService jwtService;

    @Autowired
    private ChunkService chunkService;

    @Autowired
    private UploadSessionService uploadSessionService;

    @Autowired
    private DocumentMetaDataService documentMetaDataService;

    @Autowired
    private FileProcessorFactory fileProcessorFactory;

    private static final Logger logger =  LoggerFactory.getLogger(ChunkMergeService.class);

    @Override
    public InputStream mergeChunks(HttpServletRequest request,String uploadId, int totalChunks) {

        List<InputStream> chunkStreams = new ArrayList<>();
        logger.info("Starting chunk merge for uploadId={}, totalChunks={}", uploadId, totalChunks);

        for (int i = 0; i < totalChunks; i++) {

            String chunkKey = getRedisChunkKey(uploadId, i);

            logger.info("Fetching chunk {} for uploadId={}",i,uploadId);

            InputStream chunkStream = objectStorage.get(TEMP_BUCKET, chunkKey);

            if (chunkStream == null) {
                logger.error("Missing chunk {} for uploadId={}",i,uploadId);
                throw new SPRuntimeError("Missing chunk: " + i, HttpStatus.BAD_REQUEST);
            }

            chunkStreams.add(chunkStream);
        }

        return new SequenceInputStream(Collections.enumeration(chunkStreams));
    }

    @Override
    public void mergeToFinalObject(
            HttpServletRequest request,
            String bucket,
            String objectKey,
            String uploadId,
            int totalChunks,
            UserSessionObject user,
            UploadToken payload
    ) {
        logger.info("Merging final object for uploadId={}, bucket={}, objectKey={}",uploadId, bucket,objectKey);

        String originalFileName = request.getHeader("X-File-Name");

        try (InputStream mergedStream = mergeChunks(request, uploadId, totalChunks)) {

            File tempFile = File.createTempFile("upload-", ".tmp");

            logger.info( "Temporary merged file created at path={}",tempFile.getAbsolutePath());

            try {
                try (OutputStream out = new FileOutputStream(tempFile)) {
                    mergedStream.transferTo(out);
                }

                long fileSize = tempFile.length();

                validateFileSize(fileSize, payload);
                logger.info("Validated file size={} bytes for uploadId={}",fileSize,uploadId);

                String fileHash;
                try {

                    fileHash = generateFileSignature(tempFile);
                    logger.info("Generated file hash={} for uploadId={}",fileHash,uploadId);

                } catch (Exception e) {
                    throw new SPRuntimeError("Hash generation failed", HttpStatus.INTERNAL_SERVER_ERROR);
                }

                String mimeType = resolveMimeType(tempFile);

                logger.info("Detected MIME type: uploadId={}, mimeType={}", uploadId, mimeType);

                validateMimeType(mimeType, payload);

                validateImageDimensions(tempFile, payload, mimeType);

                objectKey = objectKey.concat(resolveExtension(mimeType,originalFileName));
                String thumbnailKey;

                logger.info("Checking duplicate file for hash={} tenant={}", fileHash,payload.getTenantId());

                Optional<DocumentMetadata> existing = documentMetaDataService.findByFileHash(fileHash,payload.getTenantId());

                if (existing.isEmpty()) {
                    try (InputStream uploadStream = new FileInputStream(tempFile)) {
                        objectStorage.store(bucket, objectKey, uploadStream);
                    }

                    FileProcessor processor = fileProcessorFactory.getProcessor(mimeType);

                    logger.info("Generating thumbnail for uploadId={}",uploadId);

                    if (processor != null) {
                        try (InputStream thumbStream = new FileInputStream(tempFile)) {
                            thumbnailKey = processor.generateThumbnail(thumbStream, payload.getPersistentUploadId(), bucket);

                            if (thumbnailKey != null) {
                                long originalSize = Files.size(tempFile.toPath());
                                byte[] thumbnailBytes = objectStorage.getObjectBytes(bucket, thumbnailKey);
                                long thumbnailSize = thumbnailBytes.length;

                                if (thumbnailSize >= originalSize) {
                                    logger.info(
                                            "Thumbnail is larger than original file. Removing thumbnail. uploadId={}, originalSize={}, thumbnailSize={}",
                                            uploadId, originalSize, thumbnailSize
                                    );

                                    objectStorage.delete(bucket, thumbnailKey);
                                    thumbnailKey = null;
                                }
                            }

                        } catch (Exception e) {
                            thumbnailKey = null;
                            logger.warn("Thumbnail generation failed for uploadId={}",uploadId,e);
                        }
                    } else {
                        thumbnailKey = objectKey;
                    }
                } else {
                    logger.info("Duplicate file detected for uploadId={}, reusing existing objectKey={}",uploadId,existing.get().getObjectKey());

                    objectKey = existing.get().getObjectKey();
                    thumbnailKey = existing.get().getThumbnailKey();
                }

                payload.setFileName(originalFileName);

                logger.info("Creating metadata entry for uploadId={}",uploadId);

                documentMetaDataService.createTempDocument(
                        payload,
                        bucket,
                        objectKey,
                        mimeType,
                        fileHash,
                        thumbnailKey
                );

            } finally {
                if (tempFile.exists()) {
                    logger.debug( "Deleting temporary file path={}", tempFile.getAbsolutePath());
                    boolean delete = tempFile.delete();
                }
            }

        } catch (Exception e) {

            if(e instanceof SPRuntimeError){
                throw new SPRuntimeError(e.getMessage(), ((SPRuntimeError) e).getErrorCode());
            }

            e.printStackTrace();
            throw new SPRuntimeError("File processing failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        uploadSessionService.markCompleted(uploadId,user.getTenantId());

        deleteTempChunks(uploadId, totalChunks);
        chunkService.clearChunks(uploadId);
    }

    private void validateFileSize(long size, UploadToken payload) {

        if (size > payload.getMaxFileSize()) {
            throw new SPRuntimeError("File size exceeds the maximum limit.", HttpStatus.PAYLOAD_TOO_LARGE);
        }

        if (payload.getMinFileSize() > 0 && size < payload.getMinFileSize()) {
            throw new SPRuntimeError("Please upload a file that meets the minimum size requirement." , HttpStatus.BAD_REQUEST);
        }
    }

    private void validateMimeType(String mime, UploadToken payload) {

        if (payload.getAllowedMime() == null || payload.getAllowedMime().isEmpty()) {
            return;
        }

        if (isNull(mime) || !payload.getAllowedMime().contains(mime)) {
            throw new SPRuntimeError("Invalid file type", HttpStatus.BAD_REQUEST);
        }
    }

    private void deleteTempChunks(String uploadId, int totalChunks) {

        for (int i = 0; i < totalChunks; i++) {
            try {
                String chunkKey = getRedisChunkKey(uploadId, i);
                objectStorage.delete(TEMP_BUCKET, chunkKey);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void deleteDataChunkValidationFailure(String uploadId, Integer totalChunks){
        deleteTempChunks(uploadId, totalChunks);
        chunkService.clearChunks(uploadId);
    }

    private void validateImageDimensions(File tempFile, UploadToken payload, String mimeType) {

        if (isNull(mimeType) ||  !mimeType.startsWith("image/")) {
            return;
        }

        logger.info("Validating image dimensions for mimeType={}",mimeType);

        try {
            BufferedImage image = ImageIO.read(tempFile);

            if (image == null) {
                throw new SPRuntimeError("Invalid image file", HttpStatus.BAD_REQUEST);
            }

            int width = image.getWidth();
            int height = image.getHeight();

            logger.info("Image dimensions width={} height={}",width,height);

            if (payload.getMaxWidth() != null && width > payload.getMaxWidth()) {
                throw new SPRuntimeError("Image width exceeded", HttpStatus.BAD_REQUEST);
            }

            if (payload.getMaxHeight() != null && height > payload.getMaxHeight()) {
                throw new SPRuntimeError("Image height exceeded", HttpStatus.BAD_REQUEST);
            }

            if (payload.getMinWidth() != null && width < payload.getMinWidth()) {
                throw new SPRuntimeError("Image width too small", HttpStatus.BAD_REQUEST);
            }

            if (payload.getMinHeight() != null && height < payload.getMinHeight()) {
                throw new SPRuntimeError("Image height too small", HttpStatus.BAD_REQUEST);
            }

        } catch (IOException e) {
            throw new SPRuntimeError("Image validation failed", HttpStatus.BAD_REQUEST);
        }
    }


}
