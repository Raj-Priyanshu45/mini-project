package com.college.miniproject.Controller;

import com.college.miniproject.Dto.ChatRequest;
import com.college.miniproject.Dto.ChatResponse;
import com.college.miniproject.Dto.VoiceChatResponse;
import com.college.miniproject.Models.MoodLog;
import com.college.miniproject.Repo.MoodLogRepository;
import com.college.miniproject.Service.AudioValidator;
import com.college.miniproject.Service.ChatCompletionService;
import com.college.miniproject.Service.EmotionDetectionService;
import com.college.miniproject.Service.SpeechToTextService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.time.Instant;

@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP,
        scheme = "bearer", bearerFormat = "JWT")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatCompletionService chatService;
    private final EmotionDetectionService emotionService;
    private final MoodLogRepository moodLogRepo;
    private final SpeechToTextService speechToTextService;
    private final AudioValidator audioValidator;

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest req, Principal principal) {
        return process(userId(principal), req.message());
    }

    @Operation(summary = "Send a voice message (audio is transcribed, then handled like text chat)")
    @PostMapping(value = "/voice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VoiceChatResponse voice(
            @Parameter(description = "Audio file (wav, mp3, flac, ogg, webm, m4a; max 10MB)")
            @RequestPart("file") MultipartFile file,
            Principal principal) throws IOException {

        audioValidator.validate(file);
        String transcript = speechToTextService.transcribe(file);
        ChatResponse r = process(userId(principal), transcript);
        return new VoiceChatResponse(transcript, r.reply(), r.emotion());
    }

    private ChatResponse process(String userId, String message) {
        String emotion = emotionService.detectEmotion(message);
        moodLogRepo.save(new MoodLog(userId, emotion, Instant.now()));
        String reply = chatService.respond(userId, message, emotion);
        return new ChatResponse(reply, emotion);
    }

    private String userId(Principal principal) {
        return principal != null ? principal.getName() : "anonymous";
    }
}