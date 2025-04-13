package com.example.backend.service;

import com.example.backend.model.LogEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class LoggingService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String LOG_KEY = "api_logs";

    @Autowired
    public LoggingService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public void logToRedis(String action, String details) {
        try {
            LogEntry logEntry = new LogEntry(
                    LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
                    action,
                    details
            );
            String logJson = objectMapper.writeValueAsString(logEntry);
            redisTemplate.opsForList().leftPush(LOG_KEY, logJson);
            redisTemplate.opsForList().trim(LOG_KEY, 0, 99); // 최근 100개 로그만 유지
        } catch (JsonProcessingException e) {
            System.err.println("Redis logging error: " + e.getMessage());
        }
    }

    public List<LogEntry> getLogsFromRedis() {
        List<LogEntry> logs = new ArrayList<>();
        try {
            List<String> logJsons = redisTemplate.opsForList().range(LOG_KEY, 0, -1);
            if (logJsons != null) {
                for (String logJson : logJsons) {
                    logs.add(objectMapper.readValue(logJson, LogEntry.class));
                }
            }
        } catch (JsonProcessingException e) {
            System.err.println("Redis log retrieval error: " + e.getMessage());
        }
        return logs;
    }
} 