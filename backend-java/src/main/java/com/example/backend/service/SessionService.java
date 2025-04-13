package com.example.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class SessionService {

    private static final String SESSION_PREFIX = "session:";
    private static final long SESSION_TIMEOUT_HOURS = 24;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public String createSession(String userId) {
        String sessionId = UUID.randomUUID().toString();
        String key = SESSION_PREFIX + sessionId;
        redisTemplate.opsForValue().set(key, userId, SESSION_TIMEOUT_HOURS, TimeUnit.HOURS);
        return sessionId;
    }

    public String getUserIdFromSession(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        return redisTemplate.opsForValue().get(key);
    }

    public void invalidateSession(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        redisTemplate.delete(key);
    }

    public boolean isValidSession(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
} 