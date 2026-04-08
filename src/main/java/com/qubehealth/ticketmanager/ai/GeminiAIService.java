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
            String url = String.format(
                    "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s", model, apiKey);

            Map<String, Object> body = createRequestBody(title, description);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String responseStr = restTemplate.postForObject(url, entity, String.class);

            return parseResponse(responseStr);
        } catch (Exception e) {
            log.error("Failed to get suggestion from Gemini API: {}. Falling back to KeywordAIService.",
                    e.getMessage());
            return fallbackService.suggest(title, description);
        }
    }

    private Map<String, Object> createRequestBody(String title, String description) {
        String prompt = String.format("""
                You are an AI ticket assistant.

                Rewrite and improve the title and description.
                Then classify category and priority.
                Then generate a short summary.

                Return ONLY valid JSON.
                No markdown.
                No explanation.
                No text outside JSON.

                JSON format:
                {
                "title": "rewritten title",
                "description": "rewritten description",
                "category": "BILLING | TECHNICAL | BUG | FEATURE | ACCOUNT | OTHER",
                "priority": "LOW | MEDIUM | HIGH",
                "summary": "short summary"
                }

                Title:
                %s

                Description:
                %s
                """, title, description);

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

        JsonNode candidates = root.path("candidates");
        if (candidates.isEmpty() || candidates.get(0) == null) {
            throw new RuntimeException("No AI response candidates");
        }

        String resultText = candidates.get(0)
                .path("content")
                .path("parts")
                .get(0)
                .path("text")
                .asText();

        log.info("Gemini raw response: {}", resultText);

        // Strip markdown code fences if present
        if (resultText.contains("```json")) {
            resultText = resultText.substring(resultText.indexOf("```json") + 7);
            resultText = resultText.substring(0, resultText.indexOf("```"));
        } else if (resultText.contains("```")) {
            resultText = resultText.substring(resultText.indexOf("```") + 3);
            resultText = resultText.substring(0, resultText.indexOf("```"));
        }

        // Strip any text before the first '{' and after the last '}'
        int jsonStart = resultText.indexOf('{');
        int jsonEnd = resultText.lastIndexOf('}');
        if (jsonStart == -1 || jsonEnd == -1 || jsonEnd <= jsonStart) {
            throw new RuntimeException("No valid JSON found in Gemini response: " + resultText);
        }
        resultText = resultText.substring(jsonStart, jsonEnd + 1);

        log.info("Parsed JSON: {}", resultText);

        JsonNode jsonNode = objectMapper.readTree(resultText.trim());

        TicketCategory category;
        try {
            category = TicketCategory.valueOf(jsonNode.path("category").asText("OTHER").trim().toUpperCase());
        } catch (Exception e) {
            category = TicketCategory.OTHER;
        }

        TicketPriority priority;
        try {
            priority = TicketPriority.valueOf(jsonNode.path("priority").asText("MEDIUM").trim().toUpperCase());
        } catch (Exception e) {
            priority = TicketPriority.MEDIUM;
        }

        return new AISuggestResponse(
                category,
                priority,
                jsonNode.path("summary").asText(""),
                jsonNode.path("title").asText(""),
                jsonNode.path("description").asText(""));
    }
}
