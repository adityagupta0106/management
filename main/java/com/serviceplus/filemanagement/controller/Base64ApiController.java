package com.serviceplus.filemanagement.controller;

import com.serviceplus.filemanagement.ExceptionHandler.SPRuntimeError;
import com.serviceplus.filemanagement.dto.Base64UploadRequest;
import com.serviceplus.filemanagement.dto.UploadStatusResponse;
import com.serviceplus.filemanagement.service.UploadService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static com.serviceplus.filemanagement.Utility.Utility.getChunkSizeBytes;
import static java.util.Objects.isNull;

@RestController
@RequestMapping("/b/upload")
public class Base64ApiController {

    @Autowired
    private UploadService uploadService;

    @PostMapping("/base64")
    public ResponseEntity<?> uploadBase64File(HttpServletRequest request,
                                              @RequestHeader("X-Upload-Id") String publicUploadRef,
                                              @RequestBody Base64UploadRequest body) {

        try {

            if (isNull(body) || isNull(body.getBase64()) || body.getBase64().isBlank()) {
                throw new SPRuntimeError("Invalid base64 payload", HttpStatus.BAD_REQUEST);
            }

            String cleanBase64 = removeDataPrefix(body.getBase64());

            byte[] fileBytes = Base64.getDecoder().decode(cleanBase64);

            long chunkSize = getChunkSizeBytes();

            long totalChunks = (int) Math.ceil((double) fileBytes.length / chunkSize);

            UploadStatusResponse lastResponse = null;

            for (long chunkNumber = 0; chunkNumber < totalChunks; chunkNumber++) {

                long start = chunkNumber * chunkSize;

                long end = Math.min(start + chunkSize, fileBytes.length);

                byte[] chunk = Arrays.copyOfRange(fileBytes, (int) start, (int) end);

                InputStream chunkStream = new ByteArrayInputStream(chunk);

                lastResponse = uploadService.uploadChunk(request, publicUploadRef, (int) chunkNumber, chunkStream, (int) totalChunks);
            }

            return ResponseEntity.ok(lastResponse);

        } catch (Exception ex) {

            if (ex instanceof SPRuntimeError) {
                throw ex;
            }

            ex.printStackTrace();
            throw new SPRuntimeError("Something went wrong", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String removeDataPrefix(String base64) {

        if (base64.contains(",")) {

            return base64.substring(base64.indexOf(",") + 1);
        }

        return base64;
    }
}
