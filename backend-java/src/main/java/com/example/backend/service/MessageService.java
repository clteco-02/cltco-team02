package com.example.backend.service;

import com.example.backend.model.Message;
import com.example.backend.repository.MessageRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class MessageService {

    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);
    private static final String REDIS_CACHE_PREFIX = "message:search:";
    private static final long CACHE_TTL_MINUTES = 30;
    
    private final MessageRepository messageRepository;
    private final LoggingService loggingService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public MessageService(MessageRepository messageRepository, LoggingService loggingService, RedisTemplate<String, String> redisTemplate) {
        this.messageRepository = messageRepository;
        this.loggingService = loggingService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public Message saveMessage(String messageText) {
        Message message = new Message(messageText, LocalDateTime.now());
        Message savedMessage = messageRepository.save(message);
        loggingService.logToRedis("db_insert", "Message saved: " + 
                (messageText.length() > 30 ? messageText.substring(0, 30) + "..." : messageText));
        
        // 캐시 무효화 - 새 메시지가 저장되면 검색 결과가 변경될 수 있음
        clearSearchCache();
        
        return savedMessage;
    }

    public List<Message> getAllMessages() {
        List<Message> messages = messageRepository.findAll(
                Sort.by(Sort.Direction.DESC, "createdAt"));
        loggingService.logToRedis("db_select", "Retrieved " + messages.size() + " messages");
        return messages;
    }
    
    /**
     * 메시지 내용으로 검색
     * Redis 캐시를 먼저 확인하고, 없으면 DB에서 검색
     */
    public List<Message> searchMessages(String keyword) {
        // 검색어가 없으면 전체 메시지 반환
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllMessages();
        }
        
        String cacheKey = REDIS_CACHE_PREFIX + keyword;
        List<Message> results = new ArrayList<>();
        
        try {
            // Redis 캐시 확인
            String cachedResults = redisTemplate.opsForValue().get(cacheKey);
            
            if (cachedResults != null) {
                logger.info("Cache hit for search: {}", keyword);
                loggingService.logToRedis("cache_hit", "Search keyword: " + keyword);
                
                // 캐시된 데이터 역직렬화
                results = objectMapper.readValue(cachedResults, new TypeReference<List<Message>>() {});
                return results;
            }
            
            // 캐시에 없으면 DB에서 검색
            logger.info("Cache miss for search: {}", keyword);
            loggingService.logToRedis("cache_miss", "Search keyword: " + keyword);
            
            // 메시지 내용에서 키워드 검색
            results = messageRepository.findByMessageContainingIgnoreCase(keyword);
            
            // 검색 결과를 캐시에 저장
            try {
                String jsonResults = objectMapper.writeValueAsString(results);
                redisTemplate.opsForValue().set(cacheKey, jsonResults, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                logger.info("Cached search results for: {}", keyword);
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize search results: {}", e.getMessage());
            }
            
        } catch (RedisConnectionFailureException e) {
            // Redis 연결 실패 예외 처리
            logger.error("Redis connection failed: {}", e.getMessage());
            loggingService.logToRedis("redis_error", "Connection failed: " + e.getMessage());
            
            // Redis 실패 시 DB에서 직접 검색
            results = messageRepository.findByMessageContainingIgnoreCase(keyword);
        } catch (Exception e) {
            // 기타 예외 처리
            logger.error("Error during message search: {}", e.getMessage());
            loggingService.logToRedis("search_error", "Error: " + e.getMessage());
            
            // 예외 발생 시 DB에서 직접 검색
            results = messageRepository.findByMessageContainingIgnoreCase(keyword);
        }
        
        return results;
    }
    
    /**
     * 검색 관련 캐시 모두 삭제
     */
    private void clearSearchCache() {
        try {
            // 'message:search:' 패턴과 일치하는 모든 키 삭제
            String pattern = REDIS_CACHE_PREFIX + "*";
            for (String key : redisTemplate.keys(pattern)) {
                redisTemplate.delete(key);
            }
            logger.info("Cleared search cache");
        } catch (RedisConnectionFailureException e) {
            logger.error("Failed to clear cache - Redis connection error: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to clear cache: {}", e.getMessage());
        }
    }
} 