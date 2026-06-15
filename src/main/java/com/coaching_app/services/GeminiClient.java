package com.coaching_app.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * Thin wrapper around the Gemini generateContent API.
 * Handles 429 rate-limit retry with the same wait logic used by both
 * OcrService and RecommendationService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiClient {

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public String generate(String prompt, int maxRetries) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        String requestBody = objectMapper.writeValueAsString(Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt))
                ))
        ));

        int attempt = 0;
        int waitSeconds = 60;

        while (attempt <= maxRetries) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GEMINI_URL + geminiApiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                return root.path("candidates").get(0)
                        .path("content")
                        .path("parts").get(0)
                        .path("text")
                        .asText();
            }

            if (response.statusCode() == 429) {
                try {
                    JsonNode errorRoot = objectMapper.readTree(response.body());
                    JsonNode details = errorRoot.path("error").path("details");
                    for (JsonNode detail : details) {
                        if (detail.has("retryDelay")) {
                            String retryDelay = detail.path("retryDelay").asText();
                            waitSeconds = Integer.parseInt(retryDelay.replace("s", "")) + 10;
                        }
                    }
                } catch (Exception ignored) {}

                log.warn("Gemini rate limited (429). Waiting {}s before retry {}/{}",
                        waitSeconds, attempt + 1, maxRetries);
                Thread.sleep(waitSeconds * 1000L);
                attempt++;
                continue;
            }

            throw new RuntimeException("Gemini API error: HTTP " + response.statusCode()
                    + " — " + response.body());
        }

        throw new RuntimeException("Gemini API failed after " + maxRetries + " retries due to rate limiting.");
    }
}
