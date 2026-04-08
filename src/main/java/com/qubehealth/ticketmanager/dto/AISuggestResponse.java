package com.qubehealth.ticketmanager.dto;

import com.qubehealth.ticketmanager.entity.TicketCategory;
import com.qubehealth.ticketmanager.entity.TicketPriority;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AISuggestResponse {
    private TicketCategory category;
    private TicketPriority priority;
    private String summary;
    private String title;
    private String description;
}
