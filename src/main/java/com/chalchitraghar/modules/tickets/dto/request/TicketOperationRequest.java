package com.chalchitraghar.modules.tickets.dto.request;

import jakarta.validation.constraints.Size;

public record TicketOperationRequest(@Size(max = 500) String reason) {
    public String trimmed() {
        return reason == null ? null : reason.trim();
    }
}
