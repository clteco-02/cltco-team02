package com.example.backend.service;

import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionService sessionService;

    public String login(String id, String password) {
        Optional<User> userOpt = userRepository.findById(id);
        
        if (userOpt.isPresent() && userOpt.get().getPassword().equals(password)) {
            return sessionService.createSession(id);
        }
        
        return null;
    }

    public boolean validateSession(String sessionId) {
        return sessionService.isValidSession(sessionId);
    }

    public String getUserIdFromSession(String sessionId) {
        return sessionService.getUserIdFromSession(sessionId);
    }

    public void logout(String sessionId) {
        sessionService.invalidateSession(sessionId);
    }
} 