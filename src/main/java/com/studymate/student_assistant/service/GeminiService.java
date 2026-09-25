package com.studymate.student_assistant.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.studymate.student_assistant.model.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GeminiService {

    private final Client client;
    private final String model;

    private static final String SYSTEM_PROMPT = """
        You are "StudyMate", a friendly career and study assistant for South
        African matric students. Keep answers short (under 150 words) and
        simple English. If you don't know something, say so honestly and
        refer the student to official sources like the Department of Basic
        Education or university websites.
        """;

    public GeminiService(
            @Value("${gemini.api.key}") String apiKey,
            @Value("${gemini.api.model}") String model) {
        this.client = Client.builder().apiKey(apiKey).build();
        this.model = model;
    }

    /**
     * Send the full conversation history + new user message to Gemini.
     */
    public String askWithHistory(List<Message> history, String newUserMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append(SYSTEM_PROMPT).append("\n\n--- Conversation so far ---\n");

        for (Message m : history) {
            String who = "user".equals(m.getRole()) ? "Student" : "StudyMate";
            sb.append(who).append(": ").append(m.getContent()).append("\n");
        }
        sb.append("Student: ").append(newUserMessage).append("\nStudyMate:");

        return callWithRetry(sb.toString());
    }

    private String callWithRetry(String prompt) {
        int maxRetries = 3;
        long delayMs = 1000;
        for (int i = 1; i <= maxRetries; i++) {
            try {
                GenerateContentResponse response =
                    client.models.generateContent(model, prompt, null);
                return response.text();
            } catch (Exception e) {
                if (i == maxRetries) {
                    throw new RuntimeException(
                        "Gemini unavailable after " + maxRetries + " tries: " + e.getMessage(), e);
                }
                try { Thread.sleep(delayMs); }
                catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                delayMs *= 2;
            }
        }
        return "Sorry, the AI service is temporarily unavailable.";
    }
}