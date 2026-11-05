package com.serviceplus.filemanagement.service;

import java.time.Duration;
import java.util.Set;

public interface IRedisService {

    boolean addInSet(String key, String value);

    void setExpiration(String key, Duration duration);

    Set<String> fetchFromSet(String key);

    void delete(String key);

    Long increment(String key,Long chunkSize);

    boolean isMember(String key, String value);

    Long getSetSize(String key);
}
