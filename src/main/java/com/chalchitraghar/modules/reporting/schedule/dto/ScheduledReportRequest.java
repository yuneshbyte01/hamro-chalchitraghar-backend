package com.chalchitraghar.modules.reporting.schedule.dto;

import com.chalchitraghar.modules.reporting.enums.*;
import jakarta.validation.constraints.*;

public record ScheduledReportRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull ScheduledReportType reportType,
        @NotNull ReportScheduleFrequency scheduleFrequency,
        @NotNull ReportExportFormat deliveryFormat,
        @NotBlank @Email @Size(max = 320) String recipientEmail,
        @Pattern(regexp = "(?i)^[A-Z]{3}$") String currency,
        boolean enabled) {}
