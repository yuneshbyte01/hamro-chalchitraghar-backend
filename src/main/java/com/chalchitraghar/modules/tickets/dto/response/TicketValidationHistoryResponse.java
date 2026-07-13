package com.chalchitraghar.modules.tickets.dto.response;

import com.chalchitraghar.modules.tickets.enums.ValidationResult;
import java.time.LocalDateTime;

public record TicketValidationHistoryResponse(
        ValidationResult result,
        String reason,
        LocalDateTime validationTime,
        Long validatedById,
        String validatedByName,
        String deviceId,
        String location,
        String requestId) {}
