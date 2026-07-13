package com.chalchitraghar.modules.audit.dto.response;

import java.util.Map;

public record AuditSummaryReportResponse(
        long totalEvents,
        long successCount,
        long failureCount,
        long deniedCount,
        Map<String, Long> byCategory,
        Map<String, Long> bySeverity,
        Map<String, Long> byActorType,
        Map<String, Long> topActions,
        long systemEventCount,
        long externalEventCount) {}
