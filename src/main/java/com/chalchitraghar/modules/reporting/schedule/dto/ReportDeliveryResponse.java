package com.chalchitraghar.modules.reporting.schedule.dto;

import com.chalchitraghar.modules.reporting.enums.*;
import java.time.*;

public record ReportDeliveryResponse(
        long id,
        long scheduleId,
        LocalDate periodStart,
        LocalDate periodEnd,
        ReportExportFormat format,
        ReportDeliveryStatus status,
        int attemptCount,
        LocalDateTime lastAttemptAt,
        LocalDateTime sentAt,
        String failureReason,
        String fileName,
        Long fileSizeBytes) {}
