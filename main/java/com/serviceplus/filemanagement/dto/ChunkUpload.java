package com.serviceplus.filemanagement.dto;

import java.io.InputStream;

public class ChunkUpload {

    private String uploadId;
    private Integer chunkNumber;
    private InputStream data;
    private Integer totalChunks;
}
