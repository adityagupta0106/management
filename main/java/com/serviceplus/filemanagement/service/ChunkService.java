package com.serviceplus.filemanagement.service;

import java.io.InputStream;
import java.util.List;

public interface ChunkService {

    void markChunkReceived(String uploadId, int chunkNumber);

    List<Integer> getReceivedChunks(String uploadId);

    boolean allChunksReceived(String uploadId, int totalChunks);

    void clearChunks(String uploadId);

    public Long uploadedBytes(String key,Long chunkSize);

    boolean isChunkAlreadyUploaded(String uploadId, int chunkNumber);

    Long getChunkCount(String uploadId);
}
