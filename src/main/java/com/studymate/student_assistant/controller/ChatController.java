package com.studymate.student_assistant.controller;

import com.studymate.student_assistant.model.Conversation;
import com.studymate.student_assistant.model.CvDraft;
import com.studymate.student_assistant.model.Message;
import com.studymate.student_assistant.repository.ConversationRepository;
import com.studymate.student_assistant.repository.MessageRepository;
import com.studymate.student_assistant.service.CvService;
import com.studymate.student_assistant.service.GeminiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final GeminiService geminiService;
    private final ConversationRepository conversationRepo;
    private final MessageRepository messageRepo;
    private final CvService cvService;    // ← new

    public ChatController(GeminiService geminiService,
                          ConversationRepository conversationRepo,
                          MessageRepository messageRepo,
                          CvService cvService) {
        this.geminiService = geminiService;
        this.conversationRepo = conversationRepo;
        this.messageRepo = messageRepo;
        this.cvService = cvService;
    }

    /** Start a new conversation and return its id. */
    @PostMapping("/start")
    public Map<String, String> start(@RequestBody Map<String, String> body) {
        String sessionId = body.getOrDefault("sessionId", UUID.randomUUID().toString());
        String firstMessage = body.getOrDefault("message", "").trim();
        String title = body.getOrDefault("title",
                firstMessage.isEmpty() ? "New chat"
                        : firstMessage.substring(0, Math.min(40, firstMessage.length())));

        Conversation c = new Conversation(UUID.randomUUID().toString(), sessionId, title);
        conversationRepo.save(c);

        return Map.of("conversationId", c.getId(), "title", c.getTitle());
    }

    @GetMapping("/history")
    public List<Map<String, String>> history(@RequestParam String sessionId) {
        List<Conversation> list = conversationRepo.findBySessionIdOrderByCreatedAtDesc(sessionId);
        List<Map<String, String>> out = new ArrayList<>();
        for (Conversation c : list) {
            out.add(Map.of(
                "id", c.getId(),
                "title", c.getTitle() == null ? "New chat" : c.getTitle(),
                "createdAt", c.getCreatedAt().toString()
            ));
        }
        return out;
    }

    /** Get messages for a conversation. */
    @GetMapping("/{conversationId}/messages")
    public List<Map<String, String>> messages(@PathVariable String conversationId) {
        List<Message> list = messageRepo.findByConversationIdOrderByCreatedAtAsc(conversationId);
        List<Map<String, String>> out = new ArrayList<>();
        for (Message m : list) {
            out.add(Map.of(
                "role", m.getRole(),
                "content", m.getContent(),
                "createdAt", m.getCreatedAt().toString()
            ));
        }
        return out;
    }

    /** Send a message inside an existing conversation. */
    @PostMapping("/{conversationId}")
    public ResponseEntity<?> send(@PathVariable String conversationId,
                                  @RequestBody Map<String, String> body) {
        String userMessage = body.getOrDefault("message", "").trim();
        if (userMessage.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "message is required"));
        }

        Optional<Conversation> opt = conversationRepo.findById(conversationId);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Conversation c = opt.get();

        // Save user message
        Message userMsg = new Message(c, "user", userMessage);
        messageRepo.save(userMsg);

        // ---------- CV MODE DETECTION ----------
        String lower = userMessage.toLowerCase();

        // (1) Fresh start?
        boolean wantsCv = lower.contains("build my cv")
                       || lower.contains("make my cv")
                       || lower.contains("create my cv")
                       || lower.contains("start my cv")
                       || (lower.contains("cv") && lower.contains("help"));

        if (wantsCv) {
            CvDraft draft = cvService.startOrGet(conversationId);

            if (draft.isComplete()) {
                String msg = "Your CV is already complete! Click below to download it.";
                messageRepo.save(new Message(c, "bot", msg));
                return ResponseEntity.ok(Map.of(
                    "reply", msg,
                    "cvMode", true,
                    "complete", true,
                    "downloadUrl", "/api/cv/" + conversationId + "/download"
                ));
            }

            String q = cvService.questionFor(draft.getCurrentSlot());
            messageRepo.save(new Message(c, "bot", q));
            return ResponseEntity.ok(Map.of(
                "reply", q,
                "cvMode", true,
                "slot", draft.getCurrentSlot()
            ));
        }

        // (2) Are we in the middle of a CV draft? If yes, treat this message as an answer.
        Optional<CvDraft> draftOpt = cvService.get(conversationId);
        if (draftOpt.isPresent() && !draftOpt.get().isComplete()) {
            CvDraft draft = draftOpt.get();
            cvService.recordAnswer(draft, userMessage);

            if (draft.isComplete()) {
                String msg = "🎉 Your CV is ready! Click below to download it.";
                messageRepo.save(new Message(c, "bot", msg));
                return ResponseEntity.ok(Map.of(
                    "reply", msg,
                    "cvMode", true,
                    "complete", true,
                    "downloadUrl", "/api/cv/" + conversationId + "/download"
                ));
            }

            String q = cvService.questionFor(draft.getCurrentSlot());
            messageRepo.save(new Message(c, "bot", q));
            return ResponseEntity.ok(Map.of(
                "reply", q,
                "cvMode", true,
                "slot", draft.getCurrentSlot()
            ));
        }
        // ---------- END CV MODE ----------

        // Normal chat fallback
        List<Message> history = messageRepo
                .findByConversationIdOrderByCreatedAtAsc(conversationId);
        history.removeIf(m -> m.getId().equals(userMsg.getId()));

        String reply;
        try {
            reply = geminiService.askWithHistory(history, userMessage);
        } catch (Exception e) {
            reply = "Sorry, something went wrong: " + e.getMessage();
        }

        Message botMsg = new Message(c, "bot", reply);
        messageRepo.save(botMsg);

        return ResponseEntity.ok(Map.of(
            "reply", reply,
            "conversationId", conversationId
        ));
    }
}