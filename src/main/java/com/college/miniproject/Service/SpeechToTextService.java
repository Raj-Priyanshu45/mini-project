package com.college.miniproject.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SpeechToTextService {

    private final String pythonPath;
    private final String scriptPath;
    private final long timeoutSeconds;

//    public SpeechToTextService(
//            @Value("${stt.python-path}") String pythonPath,
//            @Value("${stt.script-path}") String scriptPath,
//            @Value("${stt.timeout-seconds:120}") long timeoutSeconds) {
//        this.pythonPath = pythonPath;
//        this.scriptPath = scriptPath;
//        this.timeoutSeconds = timeoutSeconds;
//    }

    private final String language;

    public SpeechToTextService(
            @Value("${stt.python-path}") String pythonPath,
            @Value("${stt.script-path}") String scriptPath,
            @Value("${stt.language:auto}") String language,
            @Value("${stt.timeout-seconds:120}") long timeoutSeconds) {
        this.pythonPath = pythonPath;
        this.scriptPath = scriptPath;
        this.language = language;
        this.timeoutSeconds = timeoutSeconds;
    }

    public String transcribe(MultipartFile file) {
        Path temp = null;
        try {
            String name = file.getOriginalFilename();
            String suffix = (name != null && name.contains(".")) ? name.substring(name.lastIndexOf('.')) : ".wav";
            temp = Files.createTempFile("stt-", suffix);
            file.transferTo(temp);

            ProcessBuilder pb = new ProcessBuilder(pythonPath, scriptPath, temp.toString(), language);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT); // Python errors show in the Spring console
            Process process = pb.start();

            // read stdout before waitFor so a full pipe can't block the process
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();

            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Transcription timed out");
            }
            if (process.exitValue() != 0) {
                log.error("stt_cli.py exited with {}", process.exitValue());
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Transcription failed");
            }
            if (output.isBlank()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Could not detect any speech in the audio");
            }
            return output;

        } catch (IOException e) {
            log.error("STT process error", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not run transcription");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Transcription interrupted");
        } finally {
            if (temp != null) {
                try { Files.deleteIfExists(temp); } catch (IOException ignored) {}
            }
        }
    }
}