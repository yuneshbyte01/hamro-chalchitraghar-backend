package com.chalchitraghar.modules.tickets.dto.response;
import java.time.LocalDateTime;
import com.chalchitraghar.modules.tickets.enums.ValidationResult;
public record TicketValidationHistoryResponse(ValidationResult result,String reason,LocalDateTime validationTime,
        Long validatedById,String validatedByName,String deviceId,String location,String requestId) {}
