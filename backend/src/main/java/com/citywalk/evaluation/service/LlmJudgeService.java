package com.citywalk.evaluation.service;

import com.citywalk.evaluation.config.LlmJudgeProperties;
import com.citywalk.evaluation.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class LlmJudgeService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final LlmJudgeProperties properties;

    public LlmJudgeService(RestTemplate restTemplate, ObjectMapper objectMapper, LlmJudgeProperties properties) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public JudgeDecision judge(String metric, String systemPrompt, String userPrompt) {
        if (!properties.isEnabled()) {
            return null;
        }
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            return null;
        }
        if (!"deepseek".equalsIgnoreCase(properties.getProvider())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "unsupported llm judge provider: " + properties.getProvider());
        }

        String endpoint = normalizeBaseUrl(properties.getBaseUrl()) + "/chat/completions";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());

        Map<String, Object> requestBody = Map.of(
                "model", properties.getModel(),
                "temperature", 0,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        try {
            String raw = restTemplate.postForObject(endpoint, new HttpEntity<>(requestBody, headers), String.class);
            if (raw == null || raw.isBlank()) {
                return null;
            }
            JsonNode root = objectMapper.readTree(raw);
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            if (contentNode.isMissingNode() || contentNode.asText().isBlank()) {
                return null;
            }
            JsonNode parsed = parseJsonContent(contentNode.asText());
            if (parsed == null) {
                return null;
            }
            double score = clamp(parsed.path("score").asDouble(0.0));
            String reason = parsed.path("reason").asText("llm judge returned no reason");
            return new JudgeDecision(score, reason, true);
        } catch (RestClientException ex) {
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://api.deepseek.com";
        }
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }

    private JsonNode parseJsonContent(String content) {
        try {
            return objectMapper.readTree(content);
        } catch (Exception ex) {
            int start = content.indexOf("{");
            int end = content.lastIndexOf("}");
            if (start >= 0 && end > start) {
                try {
                    return objectMapper.readTree(content.substring(start, end + 1));
                } catch (Exception ignore) {
                    return null;
                }
            }
            return null;
        }
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    public record JudgeDecision(double score, String reason, boolean llmUsed) {
    }
}
