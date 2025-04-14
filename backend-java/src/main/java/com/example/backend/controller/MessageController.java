package com.example.backend.controller;

import com.example.backend.dto.ApiStatisticsDTO;
import com.example.backend.model.Message;
import com.example.backend.service.KafkaProducerService;
import com.example.backend.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class MessageController {

    private final MessageService messageService;
    private final KafkaProducerService kafkaProducerService;

    @Autowired
    public MessageController(MessageService messageService, KafkaProducerService kafkaProducerService) {
        this.messageService = messageService;
        this.kafkaProducerService = kafkaProducerService;
    }

    @PostMapping("/db/message")
    public ResponseEntity<Map<String, String>> saveMessage(@RequestBody Map<String, String> payload, @RequestHeader(value = "X-Session-ID", required = false) String sessionId) {
        long startTime = System.currentTimeMillis();
        String status = "success";
        String messageText = payload.get("message");
        
        try {
            messageService.saveMessage(messageText);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            status = "error";
            return ResponseEntity.status(500).body(response);
        } finally {
            // API 통계 정보 Kafka로 전송
            long processingTime = System.currentTimeMillis() - startTime;
            
            ApiStatisticsDTO apiStats = new ApiStatisticsDTO(
                    "/db/message",
                    "POST",
                    sessionId != null ? sessionId : "anonymous",
                    status,
                    processingTime,
                    messageText != null ? messageText.substring(0, Math.min(messageText.length(), 50)) : ""
            );
            
            kafkaProducerService.sendApiStatistics(apiStats);
        }
    }

    @GetMapping("/db/messages")
    public ResponseEntity<?> getAllMessages(
            @RequestParam(required = false) Integer offset,
            @RequestParam(required = false) Integer limit) {
        try {
            List<Message> messages = messageService.getAllMessages();
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    @GetMapping("/db/search")
    public ResponseEntity<?> searchMessages(@RequestParam String keyword) {
        try {
            List<Message> messages = messageService.searchMessages(keyword);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
} 