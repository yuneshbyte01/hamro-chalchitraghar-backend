package com.chalchitraghar.applications.admin;

import com.chalchitraghar.modules.audit.dto.request.AdminAuditLogFilterRequest;
import com.chalchitraghar.modules.audit.dto.response.*;
import com.chalchitraghar.modules.audit.enums.*;
import com.chalchitraghar.modules.audit.factory.AuditActorResolver;
import com.chalchitraghar.modules.audit.service.*;
import com.chalchitraghar.shared.response.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(
        name = "Admin Audit Operations",
        description = "ADMIN-only bounded export, reports, and integrity verification.")
public class AdminAuditOperationsController {
    private final AuditExportService export;
    private final AuditReportService reports;
    private final AuditActorResolver actors;

    @GetMapping(value = "/audit-logs/export", produces = "text/csv")
    @Operation(summary = "Export audit logs as a bounded, formula-safe UTF-8 CSV")
    public void export(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime occurredFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime occurredTo,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) AuditCategory category,
            @RequestParam(required = false) AuditSeverity severity,
            @RequestParam(required = false) AuditResult result,
            @RequestParam(required = false) AuditActorType actorType,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false) String correlationId,
            HttpServletResponse response)
            throws java.io.IOException {
        var filter =
                new AdminAuditLogFilterRequest(
                        null,
                        null,
                        null,
                        actorType,
                        action,
                        category,
                        severity,
                        result,
                        resourceType,
                        null,
                        null,
                        requestId,
                        correlationId,
                        null,
                        null,
                        occurredFrom,
                        occurredTo);
        export.preflight(filter);
        var actor = actors.currentUserOrSystem();
        response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
        response.setContentType("text/csv");
        response.setHeader(
                HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-logs.csv\"");
        export.writeCsv(filter, actor, response.getOutputStream());
    }

    @GetMapping("/audit-reports/failed-logins")
    @Operation(summary = "Get bounded failed-login buckets")
    public ResponseEntity<ApiResponse<List<FailedLoginBucketResponse>>> failedLogins(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime occurredFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime occurredTo,
            @RequestParam(defaultValue = "DAY") AuditReportBucket bucket,
            @RequestParam(defaultValue = "1") int minimumCount) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Failed-login report fetched",
                        reports.failedLogins(occurredFrom, occurredTo, bucket, minimumCount)));
    }

    @GetMapping("/audit-reports/high-risk-actions")
    @Operation(summary = "Get paginated HIGH and CRITICAL audit actions")
    public ResponseEntity<ApiResponse<PageResponse<AdminAuditLogSummaryResponse>>> highRisk(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime occurredFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime occurredTo,
            @RequestParam(required = false) AuditCategory category,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) AuditResult result,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "High-risk report fetched",
                        reports.highRisk(
                                occurredFrom, occurredTo, category, action, result, page, size)));
    }

    @GetMapping("/audit-reports/summary")
    @Operation(summary = "Get bounded audit statistics")
    public ResponseEntity<ApiResponse<AuditSummaryReportResponse>> summary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime occurredFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime occurredTo) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Audit summary fetched", reports.summary(occurredFrom, occurredTo)));
    }

    @PostMapping("/audit-logs/integrity-check")
    @Operation(
            summary = "Verify hashes in a bounded audit range",
            description = "Detects corruption; it is not tamper-proof storage.")
    public ResponseEntity<ApiResponse<AuditIntegrityCheckResponse>> integrity(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime occurredFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime occurredTo) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Audit integrity checked", reports.integrity(occurredFrom, occurredTo)));
    }
}
