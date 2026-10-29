package com.college.miniproject.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class EmotionDetectionService {

    private final String pythonPath;
    private final String scriptPath;

    public EmotionDetectionService(
            @Value("${emotion.python-path}") String pythonPath,
            @Value("${emotion.script-path}") String scriptPath) {
        this.pythonPath = pythonPath;
        this.scriptPath = scriptPath;
    }

    public String detectEmotion(String text) {
        try {
            Process p = new ProcessBuilder(pythonPath, scriptPath, text)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (!p.waitFor(60, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return "neutral";
            }
            return (p.exitValue() == 0 && !out.isBlank()) ? out : "neutral";
        } catch (Exception e) {
            log.error("Emotion detection failed, falling back to neutral", e);
            return "neutral";
        }
    }
}