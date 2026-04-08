package com.qubehealth.ticketmanager.controller;

import com.qubehealth.ticketmanager.dto.AISuggestRequest;
import com.qubehealth.ticketmanager.dto.AISuggestResponse;
import com.qubehealth.ticketmanager.ai.AIService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "https://ticketmanager-frontend.vercel.app"})
public class AIController {

    private final AIService aiService;

    @PostMapping("/suggest")
    public ResponseEntity<AISuggestResponse> suggest(@Valid @RequestBody AISuggestRequest request) {
        AISuggestResponse response = aiService.suggest(request.getTitle(), request.getDescription());
        return ResponseEntity.ok(response);
    }
}
