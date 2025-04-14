package com.example.backend.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:20.249.104.65:9094}")
    private String bootstrapServers;
    
    @Value("${spring.kafka.security.protocol:SASL_PLAINTEXT}")
    private String securityProtocol;
    
    @Value("${spring.kafka.properties.sasl.mechanism:PLAIN}")
    private String saslMechanism;
    
    @Value("${spring.kafka.properties.sasl.jaas.config:org.apache.kafka.common.security.plain.PlainLoginModule required username=\"ss\" password=\"ss\";}")
    private String jaasConfig;
    
    @Value("${kafka.topic.api-statistics:api-statistics}")
    private String apiStatisticsTopic;
    
    @Value("${spring.kafka.producer.retries:3}")
    private int retries;
    
    @Value("${spring.kafka.producer.properties.request.timeout.ms:3000}")
    private int requestTimeoutMs;
    
    @Value("${spring.kafka.producer.properties.max.block.ms:3000}")
    private int maxBlockMs;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        // 오류 발생 시 재시도 설정
        configProps.put(ProducerConfig.RETRIES_CONFIG, retries);
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, requestTimeoutMs);
        configProps.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, maxBlockMs);
        
        // SASL 인증 설정 추가
        configProps.put("security.protocol", securityProtocol);
        configProps.put("sasl.mechanism", saslMechanism);
        configProps.put("sasl.jaas.config", jaasConfig);
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
    
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configs.put("security.protocol", securityProtocol);
        configs.put("sasl.mechanism", saslMechanism);
        configs.put("sasl.jaas.config", jaasConfig);
        return new KafkaAdmin(configs);
    }
    
    @Bean
    public NewTopic apiStatisticsTopic() {
        return TopicBuilder.name(apiStatisticsTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }
} 