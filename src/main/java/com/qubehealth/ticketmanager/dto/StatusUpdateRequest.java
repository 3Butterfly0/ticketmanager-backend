package com.qubehealth.ticketmanager.dto;

import com.qubehealth.ticketmanager.entity.TicketStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StatusUpdateRequest {
    @NotNull(message = "Status is required")
    private TicketStatus status;
}
