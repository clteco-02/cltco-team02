package com.example.backend.repository;

import com.example.backend.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    // JpaRepository에서 기본적인 CRUD 기능을 제공합니다.

    // 메시지 내용으로 검색하는 메서드
    List<Message> findByMessageContainingIgnoreCase(String keyword);
} 