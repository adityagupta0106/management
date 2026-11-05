package com.serviceplus.filemanagement.controller;

import com.serviceplus.filemanagement.dto.Base64FileResponse;
import com.serviceplus.filemanagement.dto.CommitRequest;
import com.serviceplus.filemanagement.dto.FileViewResponse;
import com.serviceplus.filemanagement.dto.SignedUrlResponse;
import com.serviceplus.filemanagement.service.implementation.FileAccessService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/b")
public class PreviewController {

    @Autowired
    private FileAccessService service;

    @GetMapping("/{uploadId}/preview")
    public ResponseEntity<SignedUrlResponse> preview(
            @PathVariable String uploadId,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(
                service.getSignedUrlByUploadId(uploadId, false,request)
        );
    }

    @GetMapping("/{uploadId}/download")
    public ResponseEntity<SignedUrlResponse> download(
            @PathVariable String uploadId,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(
                service.getSignedUrlByUploadId(uploadId, true,request)
        );
    }

    @GetMapping("/{uploadId}/thumbnail")
    public ResponseEntity<SignedUrlResponse> thumbnail(
            @PathVariable String uploadId,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(
                service.getThumbnail(uploadId, request)
        );
    }

    @PostMapping("/view/batch")
    public ResponseEntity<FileViewResponse> viewBatch(
            @RequestBody CommitRequest request,
            HttpServletRequest httpRequest
    ) {

        FileViewResponse response =
                service.getFileView(request.getUploadIds(), httpRequest);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{uploadId}/base64")
    public ResponseEntity<Base64FileResponse> getFileAsBase64(
            @PathVariable String uploadId,
            HttpServletRequest request
    ) {

        return ResponseEntity.ok(
                service.getFileAsBase64(uploadId, request)
        );
    }

}
