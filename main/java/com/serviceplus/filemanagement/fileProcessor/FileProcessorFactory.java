package com.serviceplus.filemanagement.fileProcessor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FileProcessorFactory {

    @Autowired
    private List<FileProcessor> processors;

    public FileProcessor getProcessor(String mimeType) {

        if (mimeType == null) {
            return getDefault();
        }

        if (mimeType.startsWith("image/")) {
            return getByType("image");
        }

        return processors.stream()
                .filter(p -> mimeType.equalsIgnoreCase(p.getSupportedType()))
                .findFirst()
                .orElse(getDefault());
    }

    private FileProcessor getByType(String type) {
        return processors.stream()
                .filter(p -> type.equals(p.getSupportedType()))
                .findFirst()
                .orElse(getDefault());
    }

    private FileProcessor getDefault() {
        return processors.stream()
                .filter(p -> "default".equals(p.getSupportedType()))
                .findFirst()
                .orElseThrow();
    }
}
