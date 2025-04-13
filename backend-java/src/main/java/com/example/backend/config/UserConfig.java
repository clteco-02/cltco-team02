package com.example.backend.config;

import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserConfig {

    @Bean
    public CommandLineRunner initAdminUser(UserRepository userRepository) {
        return args -> {
            // admin 계정이 없으면 생성
            if (!userRepository.findById("admin").isPresent()) {
                User admin = new User("admin", "admin");
                userRepository.save(admin);
                System.out.println("Admin user created");
            }
        };
    }
} 