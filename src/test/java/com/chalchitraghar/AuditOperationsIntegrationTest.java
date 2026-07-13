package com.chalchitraghar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.chalchitraghar.modules.audit.config.AuditOperationsProperties;
import com.chalchitraghar.modules.audit.dto.request.CreateAuditLogCommand;
import com.chalchitraghar.modules.audit.enums.*;
import com.chalchitraghar.modules.audit.service.AuditIntegrityService;
import com.chalchitraghar.modules.audit.service.AuditLogService;
import com.chalchitraghar.modules.audit.service.AuditRetentionService;
import com.chalchitraghar.modules.users.enums.Role;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AuditOperationsIntegrationTest extends AbstractIntegrationTest {
    @Autowired AuditLogService auditLogs;
    @Autowired AuditIntegrityService integrity;
    @Autowired AuditRetentionService retention;
    @Autowired AuditOperationsProperties properties;

    @Test
    void advancedFlagsAndHighRiskReportAreBoundedAndAdminOnly() throws Exception {
        append(AuditAction.ACCESS_DENIED, AuditSeverity.HIGH, AuditResult.DENIED, "safe");
        append(AuditAction.MOVIE_CREATED, AuditSeverity.INFO, AuditResult.SUCCESS, "ordinary");
        String admin = tokenFor("ops-admin@example.com", Role.ADMIN);
        mockMvc.perform(
                        get("/api/admin/audit-logs")
                                .header("Authorization", bearer(admin))
                                .param("highRiskOnly", "true")
                                .param("deniedOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
        LocalDateTime now = LocalDateTime.now(clock);
        mockMvc.perform(
                        get("/api/admin/audit-reports/high-risk-actions")
                                .header("Authorization", bearer(admin))
                                .param("occurredFrom", now.minusDays(1).toString())
                                .param("occurredTo", now.plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
        String customer = tokenFor("ops-customer@example.com", Role.CUSTOMER);
        mockMvc.perform(
                        get("/api/admin/audit-reports/summary")
                                .header("Authorization", bearer(customer))
                                .param("occurredFrom", now.minusDays(1).toString())
                                .param("occurredTo", now.plusDays(1).toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void csvIsUtf8StreamedFormulaSafeAndAuditedAfterDatasetSelection() throws Exception {
        append(
                AuditAction.LOGIN_FAILED,
                AuditSeverity.WARNING,
                AuditResult.FAILURE,
                "=SUM(A1:A2),\"bad\"\nnext");
        String admin = tokenFor("csv-admin@example.com", Role.ADMIN);
        LocalDateTime now = LocalDateTime.now(clock);
        var done =
                mockMvc.perform(
                                get("/api/admin/audit-logs/export")
                                        .header("Authorization", bearer(admin))
                                        .param("occurredFrom", now.minusDays(1).toString())
                                        .param("occurredTo", now.plusDays(1).toString()))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith("text/csv"))
                        .andReturn();
        String csv = done.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(csv)
                .contains("auditId,occurredAt")
                .contains("'=SUM(A1:A2)")
                .doesNotContain("beforeValues", "metadata");
        assertThat(auditLogRepository.countByAction(AuditAction.AUDIT_EXPORTED)).isOne();
    }

    @Test
    void summaryFailedLoginAndIntegrityReportsDoNotExposeSensitiveContent() throws Exception {
        var row =
                append(
                        AuditAction.LOGIN_FAILED,
                        AuditSeverity.WARNING,
                        AuditResult.FAILURE,
                        "INVALID_CREDENTIALS");
        assertThat(integrity.verify(auditLogRepository.findById(row.id()).orElseThrow())).isTrue();
        String admin = tokenFor("report-admin@example.com", Role.ADMIN);
        LocalDateTime now = LocalDateTime.now(clock);
        mockMvc.perform(
                        get("/api/admin/audit-reports/failed-logins")
                                .header("Authorization", bearer(admin))
                                .param("occurredFrom", now.minusDays(1).toString())
                                .param("occurredTo", now.plusDays(1).toString())
                                .param("bucket", "HOUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].total").value(1))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString("password"))));
        mockMvc.perform(
                        post("/api/admin/audit-logs/integrity-check")
                                .header("Authorization", bearer(admin))
                                .param("occurredFrom", now.minusDays(1).toString())
                                .param("occurredTo", now.plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mismatchCount").value(0));
    }

    @Test
    void exportRequiresValidBoundedRange() throws Exception {
        String admin = tokenFor("range-admin@example.com", Role.ADMIN);
        mockMvc.perform(get("/api/admin/audit-logs/export").header("Authorization", bearer(admin)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(
                        get("/api/admin/audit-logs/export")
                                .header("Authorization", bearer(admin))
                                .param("occurredFrom", "2026-01-01T00:00:00")
                                .param("occurredTo", "2026-03-01T00:00:00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void retentionAnonymizesPersonalFieldsAndPreservesImmutableCore() {
        var created =
                auditLogs.append(
                        new CreateAuditLogCommand(
                                "retention-event",
                                LocalDateTime.now(clock).minusDays(10),
                                9L,
                                "person@example.com",
                                "USER",
                                AuditActorType.USER,
                                AuditAction.MOVIE_CREATED,
                                AuditCategory.CATALOG,
                                AuditSeverity.INFO,
                                "MOVIE",
                                4L,
                                "private-reference",
                                AuditResult.SUCCESS,
                                "personal reason",
                                "request-old",
                                "correlation-old",
                                "masked-ip",
                                "agent",
                                "POST",
                                "/api/admin/movies",
                                Map.of("status", "OLD"),
                                Map.of("status", "NEW"),
                                Map.of("source", "test")));
        var config = properties.getRetention();
        config.setEnabled(true);
        config.setDefaultDays(1);
        try {
            assertThat(retention.processBatch()).isOne();
        } finally {
            config.setEnabled(false);
            config.setDefaultDays(365);
        }
        var row = auditLogRepository.findById(created.id()).orElseThrow();
        assertThat(row.getRetentionStatus()).isEqualTo(AuditRetentionStatus.ANONYMIZED);
        assertThat(row.getAnonymizedAt()).isNotNull();
        assertThat(row.getActorEmailSnapshot()).isNull();
        assertThat(row.getBeforeValues()).isNull();
        assertThat(row.getMetadata()).isNull();
        assertThat(row.getEventId()).isEqualTo("retention-event");
        assertThat(row.getAction()).isEqualTo(AuditAction.MOVIE_CREATED);
        assertThat(row.getResult()).isEqualTo(AuditResult.SUCCESS);
        assertThat(integrity.verify(row)).isTrue();
    }

    private com.chalchitraghar.modules.audit.dto.response.AdminAuditLogDetailResponse append(
            AuditAction action, AuditSeverity severity, AuditResult result, String reference) {
        return auditLogs.append(
                new CreateAuditLogCommand(
                        null,
                        LocalDateTime.now(clock),
                        null,
                        null,
                        "SYSTEM",
                        AuditActorType.SYSTEM,
                        action,
                        AuditCategory.SECURITY,
                        severity,
                        "AUDIT_TEST",
                        null,
                        reference,
                        result,
                        reference,
                        "request-test",
                        "correlation-test",
                        null,
                        null,
                        null));
    }
}
