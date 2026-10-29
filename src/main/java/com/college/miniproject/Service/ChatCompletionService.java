package com.college.miniproject.Service;

import com.college.miniproject.Models.ChatMessage;
import com.college.miniproject.Models.Conversation;
import com.college.miniproject.Repo.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ChatCompletionService {

    private final MemoryService memoryService;

    private static final String SYSTEM_PROMPT = """
            You are Virtual Saathi, a warm, non-judgmental companion for young people.
            Validate feelings before offering suggestions. Never give medical or
            clinical advice. If the user expresses self-harm intent, gently
            encourage them to reach out to a trusted adult or a helpline.
            Always reply in the same language and script the user writes in
            (for example, Hindi in Devanagari, Hinglish in Roman letters).
            """;

    private final WebClient webClient;
    private final ConversationRepository conversationRepo;
    private final String model;

    public ChatCompletionService(
            @Value("${hf.api-key}") String apiKey,
            @Value("${hf.chat-url}") String chatUrl,
            @Value("${hf.chat-model}") String model,
            ConversationRepository conversationRepo ,
            MemoryService memoryService) {
        this.model = model;
        this.conversationRepo = conversationRepo;
        this.memoryService = memoryService;
        this.webClient = WebClient.builder()
                .baseUrl(chatUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
    }

    public String respond(String userId, String userMessage, String detectedEmotion) {
        Conversation convo = conversationRepo.findById(userId).orElseGet(() -> new Conversation(userId));

        String contextualMessage = detectedEmotion != null
                ? userMessage + "\n\n[Detected user emotion: " + detectedEmotion + "]"
                : userMessage;
        convo.getMessages().add(new ChatMessage("user", contextualMessage));

        List<ChatMessage> history = convo.getMessages();
        List<ChatMessage> window = history.size() > 20
                ? history.subList(history.size() - 20, history.size())
                : history;

        String memorySummary = memoryService.getSummary(userId);

        List<Map<String, String>> payloadMessages = new ArrayList<>();
        payloadMessages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        if (!memorySummary.isBlank()) {
            payloadMessages.add(Map.of(
                    "role", "system",
                    "content", "What you remember about this specific user from past conversations:\n" + memorySummary
            ));
        }
        window.forEach(m -> payloadMessages.add(Map.of("role", m.getRole(), "content", m.getContent())));

        Map<String, Object> response = webClient.post()
                .uri("/chat/completions")
                .bodyValue(Map.of("model", model, "messages", payloadMessages))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        String reply = extractReply(response);
        convo.getMessages().add(new ChatMessage("assistant", reply));
        conversationRepo.save(convo);

        memoryService.recordTurnAndMaybeUpdate(userId, convo);

        return reply;
    }

    @SuppressWarnings("unchecked")
    private String extractReply(Map<String, Object> response) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }
}