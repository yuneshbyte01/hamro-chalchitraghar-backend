package com.chalchitraghar.applications.admin;

import com.chalchitraghar.modules.audit.dto.request.AdminAuditLogFilterRequest;
import com.chalchitraghar.modules.audit.dto.response.*;
import com.chalchitraghar.modules.audit.enums.*;
import com.chalchitraghar.modules.audit.service.AuditLogService;
import com.chalchitraghar.shared.response.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(
        name = "Admin Audit Logs",
        description =
                "ADMIN-only append-only audit reads. Audit-2 records selected authentication and business actions after commit with USER, SYSTEM, EXTERNAL, or ANONYMOUS actors. Detail-only snapshots never contain secrets; HTTP context is deferred to Audit-3.")
public class AdminAuditLogController {
    private final AuditLogService service;

    @GetMapping
    @Operation(
            summary = "List audit logs",
            description =
                    "Returns sanitized summaries, at most 100 per page, ordered by occurredAt DESC then id DESC. Supports approved identity, enum, resource, request/correlation ID, and occurrence-time filters.")
    public ResponseEntity<ApiResponse<PageResponse<AdminAuditLogSummaryResponse>>> list(
            @Parameter(description = "Zero-based page") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size from 1 to 100") @RequestParam(defaultValue = "20")
                    int size,
            @RequestParam(required = false) Long actorUserId,
            @RequestParam(required = false) String actorEmail,
            @RequestParam(required = false) String actorRole,
            @RequestParam(required = false) AuditActorType actorType,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) AuditCategory category,
            @RequestParam(required = false) AuditSeverity severity,
            @RequestParam(required = false) AuditResult result,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) Long resourceId,
            @RequestParam(required = false) String resourceReference,
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false) String correlationId,
            @RequestParam(required = false) String httpMethod,
            @RequestParam(required = false) String requestPath,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime occurredFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime occurredTo) {
        var filters =
                new AdminAuditLogFilterRequest(
                        actorUserId,
                        actorEmail,
                        actorRole,
                        actorType,
                        action,
                        category,
                        severity,
                        result,
                        resourceType,
                        resourceId,
                        resourceReference,
                        requestId,
                        correlationId,
                        httpMethod,
                        requestPath,
                        occurredFrom,
                        occurredTo);
        return ResponseEntity.ok(
                ApiResponse.success("Audit logs fetched", service.findAll(filters, page, size)));
    }

    @GetMapping("/{auditLogId}")
    @Operation(
            summary = "Get audit log detail",
            description =
                    "Returns one sanitized audit record including allowlisted snapshots; unknown IDs return 404.")
    public ResponseEntity<ApiResponse<AdminAuditLogDetailResponse>> detail(
            @PathVariable Long auditLogId) {
        return ResponseEntity.ok(
                ApiResponse.success("Audit log fetched", service.findById(auditLogId)));
    }
}
