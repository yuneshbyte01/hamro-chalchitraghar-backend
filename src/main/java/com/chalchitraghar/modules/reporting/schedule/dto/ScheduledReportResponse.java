package com.chalchitraghar.modules.reporting.schedule.dto;

import com.chalchitraghar.modules.reporting.enums.*;
import java.time.LocalDateTime;

public record ScheduledReportResponse(
        long id,
        String name,
        ScheduledReportType reportType,
        ReportScheduleFrequency scheduleFrequency,
        ReportExportFormat deliveryFormat,
        String recipientEmail,
        String currency,
        boolean enabled,
        LocalDateTime lastRunAt,
        LocalDateTime nextRunAt) {}
