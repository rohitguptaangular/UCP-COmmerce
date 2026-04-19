package com.sap.ucp.controller;

import com.sap.ucp.service.GeminiChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final GeminiChatService geminiChatService;

    public ChatController(GeminiChatService geminiChatService) {
        this.geminiChatService = geminiChatService;
    }

    @PostMapping
    public ResponseEntity<?> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String sessionId = request.getOrDefault("sessionId", UUID.randomUUID().toString());

        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message is required"));
        }

        GeminiChatService.ChatResponse response = geminiChatService.chat(sessionId, message);

        return ResponseEntity.ok(Map.of(
                "reply", response.reply(),
                "sessionId", sessionId,
                "toolCalls", response.toolCalls()
        ));
    }

    @GetMapping("/status")
    public ResponseEntity<?> status() {
        return ResponseEntity.ok(Map.of(
                "configured", geminiChatService.isConfigured(),
                "message", geminiChatService.isConfigured()
                        ? "Gemini AI is ready"
                        : "Set GEMINI_API_KEY environment variable to enable AI chat"
        ));
    }

    @PostMapping("/reset")
    public ResponseEntity<?> reset(@RequestBody Map<String, String> request) {
        String sessionId = request.get("sessionId");
        if (sessionId != null) {
            geminiChatService.clearSession(sessionId);
        }
        return ResponseEntity.ok(Map.of("status", "Session cleared"));
    }
}
