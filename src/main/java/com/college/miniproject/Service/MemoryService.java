package com.college.miniproject.Service;

import com.college.miniproject.Models.ChatMessage;
import com.college.miniproject.Models.Conversation;
import com.college.miniproject.Models.UserMemory;
import com.college.miniproject.Repo.UserMemoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class MemoryService {

    private static final int SUMMARY_TRIGGER = 3; // regenerate memory every 3 user turns — lower for demo visibility

    private static final String MEMORY_PROMPT = """
            You maintain a short, private memory profile of one user for a companion app.
            Given the EXISTING memory notes and the RECENT conversation, produce an updated
            memory profile: 3-6 short bullet points capturing durable facts, ongoing
            concerns, or context worth remembering (e.g. "preparing for exams in April",
            "runs a small business", "prefers direct advice over sympathy").
            Do not include transient small talk. Do not repeat duplicate points.
            Return ONLY the bullet points, nothing else.
            """;

    private final WebClient webClient;
    private final String model;
    private final UserMemoryRepository memoryRepo;

    public MemoryService(
            @Value("${hf.api-key}") String apiKey,
            @Value("${hf.chat-url}") String chatUrl,
            @Value("${hf.chat-model}") String model,
            UserMemoryRepository memoryRepo) {
        this.model = model;
        this.memoryRepo = memoryRepo;
        this.webClient = WebClient.builder()
                .baseUrl(chatUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
    }

    public String getSummary(String userId) {
        return memoryRepo.findById(userId).map(UserMemory::getSummary).orElse("");
    }

    public void recordTurnAndMaybeUpdate(String userId, Conversation convo) {
        UserMemory memory = memoryRepo.findById(userId).orElseGet(() -> {
            UserMemory m = new UserMemory();
            m.setUserId(userId);
            return m;
        });

        memory.setMessagesSinceLastSummary(memory.getMessagesSinceLastSummary() + 1);

        if (memory.getMessagesSinceLastSummary() >= SUMMARY_TRIGGER) {
            memory.setSummary(regenerateSummary(memory.getSummary(), convo));
            memory.setMessagesSinceLastSummary(0);
        }

        UserMemory saved = memoryRepo.save(memory);
        System.out.println("[MemoryService] saved user_memory for " + userId + " -> " + saved); // TEMP: confirm this fires
    }

    private String regenerateSummary(String existingSummary, Conversation convo) {
        List<ChatMessage> recent = convo.getMessages().size() > 12
                ? convo.getMessages().subList(convo.getMessages().size() - 12, convo.getMessages().size())
                : convo.getMessages();

        StringBuilder transcript = new StringBuilder();
        recent.forEach(m -> transcript.append(m.getRole()).append(": ").append(m.getContent()).append("\n"));

        List<Map<String, String>> payloadMessages = new ArrayList<>();
        payloadMessages.add(Map.of("role", "system", "content", MEMORY_PROMPT));
        payloadMessages.add(Map.of(
                "role", "user",
                "content", "EXISTING memory notes:\n" + (existingSummary.isBlank() ? "(none yet)" : existingSummary)
                        + "\n\nRECENT conversation:\n" + transcript
        ));

        Map<String, Object> response = webClient.post()
                .uri("/chat/completions")
                .bodyValue(Map.of("model", model, "messages", payloadMessages))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        return extractReply(response);
    }

    @SuppressWarnings("unchecked")
    private String extractReply(Map<String, Object> response) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }
}