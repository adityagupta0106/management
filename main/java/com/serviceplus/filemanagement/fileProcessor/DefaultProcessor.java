package com.serviceplus.filemanagement.fileProcessor;

import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class DefaultProcessor  implements FileProcessor {

    @Override
    public String generateThumbnail(InputStream fileBytes, String uploadId, String bucket) {
        return null;
    }

    @Override
    public String getSupportedType() {
        return "default";
    }
}
