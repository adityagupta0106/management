package com.serviceplus.filemanagement.storage;

import io.minio.http.Method;

import java.io.InputStream;
import java.util.Map;

public interface ObjectStorage {

    Long store(String bucket,String objectKey, InputStream data);
    InputStream get(String bucket,String objectKey);
    void delete(String bucket,String objectKey);
    boolean bucketExists(String bucket);
    void createBucket(String bucket);
    String getPresignedUrl(
            String baseUrl,
            String bucket,
            String objectKey,
            String fileName,
            String contentType,
            boolean download,
            int expirySeconds,
            Map<String, String> param,
            Method httpMethod
    );

    byte[] getObjectBytes(
            String bucketName,
            String objectKey
    );

}
