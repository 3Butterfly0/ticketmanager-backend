package com.qubehealth.ticketmanager.ai;
import com.qubehealth.ticketmanager.dto.AISuggestResponse;

public interface AIService {
    // suggests category, priority and summary based on ticket title and description
    AISuggestResponse suggest(String title, String description);
}
