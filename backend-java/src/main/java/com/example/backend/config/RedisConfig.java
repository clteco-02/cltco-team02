package com.example.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
//
//@Configuration
//@Profile("!local")
//public class RedisConfig {
//
//    @Bean
//    public RedisConnectionFactory redisConnectionFactory(
//            @Value("${spring.redis.host:localhost}") String host,
//            @Value("${spring.redis.port:6379}") int port,
//            @Value("${spring.redis.password:}") String password,
//            @Value("${spring.redis.username:}") String username) {
//        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
//        redisConfig.setHostName(host);
//        redisConfig.setPort(port);
//        redisConfig.setUsername(username);
//        if (password != null && !password.isEmpty()) {
//            redisConfig.setPassword(password);
//        }
//        System.out.println("Connecting to Redis at: " + host + ":" + port);
//        return new LettuceConnectionFactory(redisConfig);
//    }
//
//    @Bean
//    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
//        System.out.println("dev!");
//        RedisTemplate<String, String> template = new RedisTemplate<>();
//        template.setConnectionFactory(connectionFactory);
//        template.setKeySerializer(new StringRedisSerializer());
//        template.setValueSerializer(new StringRedisSerializer());
//        return template;
//    }
//}

@Configuration
@Profile("!local")
public class RedisConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory(
            @Value("${spring.redis.host}") String host,
            @Value("${spring.redis.port}") int port,
            @Value("${spring.redis.password:}") String password,
            @Value("${spring.redis.username:}") String username,
            @Value("${spring.redis.ssl:false}") boolean sslEnabled
    ) {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(host);
        redisConfig.setPort(port);

        // username/password 설정
        if (username != null && !username.isEmpty()) {
            redisConfig.setUsername(username);
        }
        if (password != null && !password.isEmpty()) {
            redisConfig.setPassword(password);
        }

        // SSL 옵션 설정
        LettuceClientConfiguration.LettuceClientConfigurationBuilder builder =
                LettuceClientConfiguration.builder();

        if (sslEnabled) {
            builder.useSsl();
        }

        LettuceClientConfiguration lettuceClientConfiguration = builder.build();

        System.out.println("Connecting to Redis at: " + host + ":" + port
                + " with SSL: " + sslEnabled);

        return new LettuceConnectionFactory(redisConfig, lettuceClientConfiguration);
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        System.out.println("dev!");
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
}
