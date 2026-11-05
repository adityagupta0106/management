package com.serviceplus.filemanagement.controller;

import com.serviceplus.filemanagement.dto.CommitRequest;
import com.serviceplus.filemanagement.dto.CommitResponse;
import com.serviceplus.filemanagement.service.implementation.FileCommitService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/b/files")
public class CommitController {

    @Autowired
    private FileCommitService commitService;

    @PostMapping("/commit")
    public ResponseEntity<?> commitBatch(
            @RequestBody CommitRequest request, HttpServletRequest servletRequest
            ) {

        List<CommitResponse.CommitResult> response = commitService.commitBatch(request.getUploadIds(),servletRequest);

        return ResponseEntity.ok(new CommitResponse(response));
    }
}
