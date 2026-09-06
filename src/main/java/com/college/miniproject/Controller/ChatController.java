package com.college.miniproject.Controller;

import com.college.miniproject.Service.CompanionChatService;
import com.college.miniproject.Service.EmotionDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.Instant;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final CompanionChatService chatService;
    private final EmotionDetectionService emotionService;
    private final MoodLogRepository moodLogRepo;

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest req, Principal principal) {
        String emotion = emotionService.detectEmotion(req.message());
        moodLogRepo.save(new MoodLog(principal.getName(), emotion, Instant.now()));
        String reply = chatService.respond(principal.getName(), req.message(), emotion);
        return new ChatResponse(reply, emotion);
    }
}