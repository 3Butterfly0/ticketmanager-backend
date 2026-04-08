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
        String prompt = String.format(
                """
                        You are an AI that rewrites support tickets professionally.

                        Rewrite the title and description to be clearer, more professional, and concise.
                        Do NOT keep original wording.

                        Example:

                        Input Title:
                        payment issue with client

                        Output Title:
                        Client payment completed but status not updated

                        Input Description:
                        payment was done by client but response came late

                        Output Description:
                        The client's payment was successfully processed, but the response was received after the application's timeout, causing the payment status to remain pending.

                        Now rewrite the following:

                        Title:
                        %s

                        Description:
                        %s

                        Also classify:

                        Categories:
                        BILLING, TECHNICAL, BUG, FEATURE, ACCOUNT, OTHER

                        Priority:
                        HIGH = payment failure, blocking issue
                        MEDIUM = delay, partial issue
                        LOW = UI or enhancement

                        Return ONLY JSON:

                        {
                        "title": "",
                        "description": "",
                        "category": "",
                        "priority": "",
                        "summary": ""
                        }

                        Summary must be 1 short sentence.
                        """,
                title, description);

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", prompt);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", Collections.singletonList(textPart));

        Map<String, Object> body = new HashMap<>();
        body.put("contents", Collections.singletonList(content));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 1);
        generationConfig.put("topP", 0.9);
        generationConfig.put("maxOutputTokens", 1000);

        body.put("generationConfig", generationConfig);

        return body;
    }

    private AISuggestResponse parseResponse(String responseStr) throws Exception {
        JsonNode root = objectMapper.readTree(responseStr);

        JsonNode parts = root
                .path("candidates")
                .get(0)
                .path("content")
                .path("parts");

        StringBuilder fullText = new StringBuilder();

        for (JsonNode part : parts) {
            fullText.append(part.path("text").asText());
        }

        String resultText = fullText.toString().trim();

        log.info("Gemini raw response: {}", resultText);

        // Extract JSON block
        int start = resultText.indexOf("{");
        int end = resultText.lastIndexOf("}");

        if (start == -1 || end == -1) {
            throw new RuntimeException("Invalid AI response: " + resultText);
        }

        String json = resultText.substring(start, end + 1);

        JsonNode jsonNode = objectMapper.readTree(json);

        return new AISuggestResponse(
                TicketCategory.valueOf(jsonNode.path("category").asText("OTHER").toUpperCase()),
                TicketPriority.valueOf(jsonNode.path("priority").asText("MEDIUM").toUpperCase()),
                jsonNode.path("summary").asText(""),
                jsonNode.path("title").asText(""),
                jsonNode.path("description").asText(""));
    }
}
