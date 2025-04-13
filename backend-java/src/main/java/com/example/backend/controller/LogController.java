package com.example.backend.controller;

import com.example.backend.model.LogEntry;
import com.example.backend.service.LoggingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class LogController {

    private final LoggingService loggingService;

    @Autowired
    public LogController(LoggingService loggingService) {
        this.loggingService = loggingService;
    }

    @GetMapping("/logs/redis")
    public ResponseEntity<?> getRedisLogs() {
        try {
            List<LogEntry> logs = loggingService.getLogsFromRedis();
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
} 