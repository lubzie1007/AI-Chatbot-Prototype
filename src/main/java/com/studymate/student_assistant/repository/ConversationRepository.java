package com.studymate.student_assistant.repository;

import com.studymate.student_assistant.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, String> {
    List<Conversation> findBySessionIdOrderByCreatedAtDesc(String sessionId);
}