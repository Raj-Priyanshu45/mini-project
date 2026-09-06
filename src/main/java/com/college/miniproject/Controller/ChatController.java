package com.college.miniproject.Controller;

import com.college.miniproject.Dto.ChatRequest;
import com.college.miniproject.Dto.ChatResponse;
import com.college.miniproject.Models.MoodLog;
import com.college.miniproject.Repo.MoodLogRepository;
import com.college.miniproject.Service.ChatCompletionService;
import com.college.miniproject.Service.EmotionDetectionService;
import lombok.RequiredArgsConstructor;
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

    private final ChatCompletionService chatService;   // was CompanionChatService
    private final EmotionDetectionService emotionService;
    private final MoodLogRepository moodLogRepo;

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest req, Principal principal) {
        String userId = principal != null ? principal.getName() : "anonymous";
        String emotion = emotionService.detectEmotion(req.message());
        moodLogRepo.save(new MoodLog(userId, emotion, Instant.now()));
        String reply = chatService.respond(userId, req.message(), emotion);
        return new ChatResponse(reply, emotion);
    }
}