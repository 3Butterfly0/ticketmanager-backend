package com.qubehealth.ticketmanager.ai;
import com.qubehealth.ticketmanager.dto.AISuggestResponse;
import com.qubehealth.ticketmanager.entity.TicketCategory;
import com.qubehealth.ticketmanager.entity.TicketPriority;
import org.springframework.stereotype.Service;

@Service
public class KeywordAIService implements AIService {

    @Override
    public AISuggestResponse suggest(String title, String description) {
        String content = (title + " " + description).toLowerCase();

        return new AISuggestResponse(
            determineCategory(content),
            determinePriority(content),
            generateSummary(description)
        );
    }

    private TicketCategory determineCategory(String content) {
        if (content.contains("payment") || content.contains("bill") || content.contains("charge") || content.contains("refund")) {
            return TicketCategory.BILLING;
        }
        if (content.contains("error") || content.contains("crash") || content.contains("bug") || content.contains("broken") || content.contains("not working")) {
            return TicketCategory.BUG;
        }
        if (content.contains("slow") || content.contains("server") || content.contains("api") || content.contains("setup") || content.contains("performance")) {
            return TicketCategory.TECHNICAL;
        }
        if (content.contains("feature") || content.contains("request") || content.contains("add") || content.contains("improve")) {
            return TicketCategory.FEATURE;
        }
        if (content.contains("account") || content.contains("login") || content.contains("password") || content.contains("profile")) {
            return TicketCategory.ACCOUNT;
        }
        return TicketCategory.OTHER;
    }

    private TicketPriority determinePriority(String content) {
        if (content.contains("urgent") || content.contains("critical") || content.contains("asap") || content.contains("emergency") || content.contains("broken")) {
            return TicketPriority.HIGH;
        }
        if (content.contains("low") || content.contains("minor") || content.contains("whenever") || content.contains("suggestion")) {
            return TicketPriority.LOW;
        }
        return TicketPriority.MEDIUM;
    }

    private String generateSummary(String description) {
        if (description == null || description.isEmpty()) return "";
        int end = Math.min(description.length(), 100);
        return description.substring(0, end) + (description.length() > 100 ? "..." : "");
    }
}
