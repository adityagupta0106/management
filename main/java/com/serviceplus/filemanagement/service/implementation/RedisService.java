package com.serviceplus.filemanagement.service.implementation;

import com.serviceplus.filemanagement.service.IRedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

@Service
@Primary
public class RedisService implements IRedisService {

    @Autowired
    private RedisTemplate<String, String> template;

    @Override
    public boolean addInSet(String key, String value) {
        try{
            Long res = template.opsForSet().add(key, value);
            return res != null && res > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void setExpiration(String key, Duration duration){
        try{
            template.expire(key, duration);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Set<String> fetchFromSet(String key){
        try{
            return template.opsForSet().members(key);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void delete(String key){
        try{
            template.delete(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Long increment(String key, Long chunkSize) {
        try{
            return template.opsForValue().increment(key, chunkSize);
        } catch (Exception e) {
            e.printStackTrace();
           return -1L;
        }
    }

    @Override
    public boolean isMember(String key, String value) {
        try {
            Boolean result = template.opsForSet().isMember(key, value);
            return result != null && result;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Long getSetSize(String key){
        try{
            return template.opsForSet().size(key);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
