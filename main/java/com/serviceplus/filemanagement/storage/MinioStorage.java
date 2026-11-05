package com.serviceplus.filemanagement.storage;

import com.serviceplus.filemanagement.ExceptionHandler.SPRuntimeError;
import com.serviceplus.filemanagement.Utility.CountingInputStream;
import io.minio.*;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URI;
import java.util.Map;

@Service
public class MinioStorage implements ObjectStorage{

    @Autowired
    private MinioClient minioClient;

    @Override
    public Long store(String bucket, String objectKey, InputStream data) {

        try {
            CountingInputStream countingStream = new CountingInputStream(data);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .stream(countingStream, -1, 5 * 1024 * 1024)
                            .contentType("application/octet-stream")
                            .build()
            );

            return countingStream.getBytesRead();

        } catch (Exception e) {
            e.printStackTrace();
            throw new SPRuntimeError("OBJ [01]", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public InputStream get(String bucket,String objectKey) {

        try {

            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build()
            );

        } catch (Exception e) {
            e.printStackTrace();
            throw new SPRuntimeError("OBJ [02]", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void delete(String bucket,String objectKey) {

        try {

            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build()
            );

        } catch (Exception e) {
            e.printStackTrace();
            throw new SPRuntimeError("OBJ [03]", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public boolean bucketExists(String bucket) {
        try {
            return minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Bucket check failed", e);
        }
    }

    @Override
    public void createBucket(String bucket) {
        try {
            minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket(bucket).build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Bucket creation failed", e);
        }
    }

    @Override
    public String getPresignedUrl(
            String baseUrl,
            String bucket,
            String objectKey,
            String fileName,
            String contentType,
            boolean download,
            int expirySeconds,
            Map<String, String> param,
            Method httpMethod
    ) {

        try {

            String internalUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(httpMethod)
                            .bucket(bucket)
                            .object(objectKey)
                            .expiry(expirySeconds)
                            .extraQueryParams(param)
                            .build()
            );

            int pathStartIndex = internalUrl.indexOf("/", internalUrl.indexOf("//") + 2);

            String pathAndQuery = internalUrl.substring(pathStartIndex);

            return baseUrl + pathAndQuery;

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate signed URL", e);
        }
    }

    @Override
    public byte[] getObjectBytes(String bucketName, String objectKey) {

        try (InputStream inputStream =
                     minioClient.getObject(
                             GetObjectArgs.builder()
                                     .bucket(bucketName)
                                     .object(objectKey)
                                     .build()
                     )) {

            return inputStream.readAllBytes();

        } catch (Exception e) {
            throw new RuntimeException("Failed to read object from storage", e);
        }
    }

}
