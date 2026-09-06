package com.college.miniproject.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
@Service
public class EmotionDetectionService {

    private final WebClient webClient;
    private final String modelUri;

    public EmotionDetectionService(
            @Value("${hf.api-key}") String apiKey,
            @Value("${hf.uri}") String hfUri,
            @Value("${hf.model-uri}") String modelUri
    ) {
        this.modelUri = modelUri;

        this.webClient = WebClient.builder()
                .baseUrl(hfUri)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
    }

    public String detectEmotion(String text) {

        List<List<Map<String, Object>>> response = webClient.post()
                .uri(modelUri)
                .bodyValue(Map.of("inputs", text))
                .retrieve()
                .bodyToMono(
                        new ParameterizedTypeReference<List<List<Map<String, Object>>>>() {}
                )
                .block();

        return response.get(0).stream()
                .max(Comparator.comparingDouble(
                        m -> (Double) m.get("score")
                ))
                .map(m -> (String) m.get("label"))
                .orElse("neutral");
    }
}