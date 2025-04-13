package com.example.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.URI;

@Configuration
@Profile("local")
public class TestRedisConfig {

    @Value("${spring.redis.url}")
    private String redisUrl;

    @Value("${spring.redis.password}")
    private String redisPassword;

    @Value("${spring.redis.database:0}")
    private int redisDatabase;

    private GenericContainer<?> redisContainer;

    @PostConstruct
    public void startRedisContainer() {
        redisContainer = new GenericContainer<>("redis:7.0.5")
                .withExposedPorts(6379)
                .withCommand("redis-server", "--requirepass", redisPassword)
                .waitingFor(Wait.forListeningPort());
        redisContainer.start();
    }

    @PreDestroy
    public void stopRedisContainer() {
        if (redisContainer != null) {
            redisContainer.stop();
        }
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();

        try {
            if (redisContainer != null && redisContainer.isRunning()) {
                // 테스트 환경: Testcontainers 사용
                config.setHostName(redisContainer.getHost());
                config.setPort(redisContainer.getMappedPort(6379));
            } else {
                // 운영 환경: 외부 Redis 서버 사용
                URI uri = new URI(redisUrl);
                config.setHostName(uri.getHost());
                config.setPort(uri.getPort());
            }

            config.setPassword(redisPassword);
            config.setDatabase(redisDatabase);
        } catch (Exception e) {
            throw new RuntimeException("Failed to configure Redis connection", e);
        }

        return new LettuceConnectionFactory(config);
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        System.out.println("local!");
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}