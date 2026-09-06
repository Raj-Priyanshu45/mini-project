package com.college.miniproject.Service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

@Service
public class CompanionChatService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    public CompanionChatService(ChatClient.Builder builder, ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
        this.chatClient = builder
                .defaultSystem("""
                You are Virtual Saathi, a warm, non-judgmental companion for young people.
                Validate feelings before offering suggestions. Never give medical or
                clinical advice. If the user expresses self-harm intent, gently
                encourage them to reach out to a trusted adult or a helpline.
                """)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    public String respond(String userId, String message, String detectedEmotion) {
        String contextualPrompt = detectedEmotion != null
                ? message + "\n\n[Detected user emotion: " + detectedEmotion + "]"
                : message;

        return chatClient.prompt()
                .user(contextualPrompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userId))
                .call()
                .content();
    }
}