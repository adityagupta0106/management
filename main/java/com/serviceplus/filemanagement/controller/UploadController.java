package com.serviceplus.filemanagement.controller;

import com.serviceplus.filemanagement.ExceptionHandler.SPRuntimeError;
import com.serviceplus.filemanagement.dto.CancelResult;
import com.serviceplus.filemanagement.dto.ReplaceFileRequest;
import com.serviceplus.filemanagement.dto.UploadStatusResponse;
import com.serviceplus.filemanagement.service.ChunkService;
import com.serviceplus.filemanagement.service.UploadService;
import com.serviceplus.filemanagement.service.UploadSessionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/upload")
public class UploadController {

    @Autowired
    private UploadService uploadService;

    @Autowired
    private UploadSessionService uploadSessionService;

    @PostMapping("/chunk")
    public ResponseEntity<?> uploadChunk(
            HttpServletRequest request,
            @RequestHeader("X-Upload-Id") String uploadId,
            @RequestHeader("X-Chunk-Number") int chunkNumber,
            @RequestHeader("X-Total-Chunks") int totalChunks
    )throws Exception{

        try {
            InputStream data = request.getInputStream();
            UploadStatusResponse uploaded = uploadService.uploadChunk(request,
                    uploadId,
                    chunkNumber,
                    data,
                    totalChunks);

            Map<String, UploadStatusResponse> result = new HashMap<>();
            result.put("data", uploaded);
            return ResponseEntity.ok().body(result);

        } catch (Exception e) {
            if (e instanceof SPRuntimeError) {
                throw e;
            } else {
                e.printStackTrace();
                throw new SPRuntimeError("Something went wrong", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
    }

    @PostMapping("/replace")
    public ResponseEntity<?> replaceFile(
            HttpServletRequest request,@RequestParam String uploadRef
    ) {

        return uploadSessionService.replaceSession(request,uploadRef);
    }
}
