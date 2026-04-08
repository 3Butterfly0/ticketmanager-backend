package com.qubehealth.ticketmanager.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qubehealth.ticketmanager.dto.AISuggestResponse;
import com.qubehealth.ticketmanager.entity.TicketCategory;
import com.qubehealth.ticketmanager.entity.TicketPriority;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Primary
@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiAIService implements AIService {

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.model}")
    private String model;

    private final RestTemplate restTemplate;
    private final KeywordAIService fallbackService;
    // private final ObjectMapper objectMapper; // not supported in spring 4
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public AISuggestResponse suggest(String title, String description) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Gemini API key is not configured. Falling back to KeywordAIService.");
            return fallbackService.suggest(title, description);
        }

        try {
            String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s", model, apiKey);

            Map<String, Object> body = createRequestBody(title, description);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String responseStr = restTemplate.postForObject(url, entity, String.class);

            return parseResponse(responseStr);
        } catch (Exception e) {
            log.error("Failed to get suggestion from Gemini API: {}. Falling back to KeywordAIService.", e.getMessage());
            return fallbackService.suggest(title, description);
        }
    }

    private Map<String, Object> createRequestBody(String title, String description) {
        String prompt = String.format(
            "Analyze this support ticket and suggest a category, priority, and a short one-line summary (max 100 chars). " +
            "Category must be one of: BILLING, TECHNICAL, BUG, FEATURE, ACCOUNT, OTHER. " +
            "Priority must be one of: LOW, MEDIUM, HIGH. " +
            "Return only a plain JSON object with properties 'category', 'priority', and 'summary'. " +
            "Ticket Title: %s. Ticket Description: %s",
            title, description);

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", prompt);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", Collections.singletonList(textPart));

        Map<String, Object> body = new HashMap<>();
        body.put("contents", Collections.singletonList(content));
        return body;
    }

    private AISuggestResponse parseResponse(String responseStr) throws Exception {
        JsonNode root = objectMapper.readTree(responseStr);
        String resultText = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
        
        if (resultText.contains("```json")) {
            resultText = resultText.substring(resultText.indexOf("```json") + 7);
            resultText = resultText.substring(0, resultText.indexOf("```"));
        } else if (resultText.contains("```")) {
            resultText = resultText.substring(resultText.indexOf("```") + 3);
            resultText = resultText.substring(0, resultText.indexOf("```"));
        }

        JsonNode jsonNode = objectMapper.readTree(resultText.trim());

        return new AISuggestResponse(
            TicketCategory.valueOf(jsonNode.path("category").asText("OTHER").toUpperCase()),
            TicketPriority.valueOf(jsonNode.path("priority").asText("MEDIUM").toUpperCase()),
            jsonNode.path("summary").asText("")
        );
    }
}
