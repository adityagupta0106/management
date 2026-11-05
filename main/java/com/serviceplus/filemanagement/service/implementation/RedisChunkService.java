package com.serviceplus.filemanagement.service.implementation;

import com.serviceplus.filemanagement.service.ChunkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;

import static com.serviceplus.filemanagement.Utility.ApplicationConstants.CHUNK_EXPIRATION_REDIS_TTL_HOURS;

@Service
public class RedisChunkService implements ChunkService {

    @Autowired
    private RedisService redisService;

    private String chunkKey(String uploadId) {
        return "UPLOAD:CHUNKS:" + uploadId;
    }

    private String sizeKey(String uploadId) {
        return "UPLOAD:SIZE:" + uploadId;
    }

    @Override
    public void markChunkReceived(String uploadId, int chunkNumber) {
        redisService.addInSet(chunkKey(uploadId), String.valueOf(chunkNumber));
        redisService.setExpiration(chunkKey(uploadId), Duration.ofHours(CHUNK_EXPIRATION_REDIS_TTL_HOURS));
    }

    @Override
    public boolean isChunkAlreadyUploaded(String uploadId, int chunkNumber) {
        return redisService.isMember(chunkKey(uploadId), String.valueOf(chunkNumber));
    }

    @Override
    public List<Integer> getReceivedChunks(String uploadId) {
        return redisService.fetchFromSet(chunkKey(uploadId)).stream()
                .map(Integer::parseInt)
                .sorted()
                .toList();
    }

    @Override
    public Long getChunkCount(String uploadId){
        return redisService.getSetSize(chunkKey(uploadId));
    }

    @Override
    public boolean allChunksReceived(String uploadId, int totalChunks) {
        Long size = getChunkCount(uploadId);
        return size.intValue() == totalChunks;
    }

    @Override
    public void clearChunks(String uploadId) {
        redisService.delete(chunkKey(uploadId));
        redisService.delete(sizeKey(uploadId));
    }

    @Override
    public Long uploadedBytes(String uploadId, Long chunkSize) {
        Long total = redisService.increment(sizeKey(uploadId), chunkSize);
        redisService.setExpiration(sizeKey(uploadId), Duration.ofHours(CHUNK_EXPIRATION_REDIS_TTL_HOURS));
        return total;
    }
}
