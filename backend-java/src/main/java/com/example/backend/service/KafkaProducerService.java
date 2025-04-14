package com.example.backend.service;

import com.example.backend.dto.ApiStatisticsDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class KafkaProducerService {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);
    private static final int SEND_TIMEOUT_SECONDS = 2; // 2초 타임아웃
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String apiStatisticsTopic;
    
    @Autowired
    public KafkaProducerService(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${kafka.topic.api-statistics:api-statistics}") String apiStatisticsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.apiStatisticsTopic = apiStatisticsTopic;
    }
    
    public void sendApiStatistics(ApiStatisticsDTO statistics) {
        try {
            // 비동기 전송하되 짧은 타임아웃 설정
            ListenableFuture<SendResult<String, Object>> future = 
                    kafkaTemplate.send(apiStatisticsTopic, statistics.getApiEndpoint(), statistics);
            
            // 비동기 콜백 등록
            future.addCallback(new ListenableFutureCallback<SendResult<String, Object>>() {
                @Override
                public void onSuccess(SendResult<String, Object> result) {
                    logger.info("Sent API statistics to Kafka: {} with offset: {}", 
                            statistics.getApiEndpoint(), 
                            result.getRecordMetadata().offset());
                }
                
                @Override
                public void onFailure(Throwable ex) {
                    logger.warn("Unable to send API statistics to Kafka: {}, Error: {}", 
                            statistics.getApiEndpoint(), ex.getMessage());
                }
            });
            
            // 짧은 타임아웃으로 전송 시도 (선택적)
            try {
                future.get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                logger.warn("Kafka send operation timed out after {} seconds - application will continue", 
                        SEND_TIMEOUT_SECONDS);
            } catch (Exception e) {
                logger.warn("Kafka send operation failed - application will continue: {}", 
                        e.getMessage());
            }
            
        } catch (KafkaException e) {
            logger.error("Failed to configure Kafka send operation: {}", e.getMessage());
            // Kafka 실패는 애플리케이션 기능에 영향을 주지 않도록 예외를 잡음
        } catch (Exception e) {
            logger.error("Unexpected error during Kafka send operation: {}", e.getMessage());
            // 모든 예외를 잡아서 애플리케이션 동작에 영향을 주지 않도록 함
        }
    }
} 