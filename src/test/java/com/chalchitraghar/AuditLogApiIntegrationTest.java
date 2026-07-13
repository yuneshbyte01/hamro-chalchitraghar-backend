package com.chalchitraghar;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.chalchitraghar.modules.audit.dto.request.CreateAuditLogCommand;
import com.chalchitraghar.modules.audit.enums.*;
import com.chalchitraghar.modules.audit.service.AuditLogService;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class AuditLogApiIntegrationTest extends AbstractIntegrationTest {
    @Autowired AuditLogService auditLogService;

    @Test
    void adminListsFiltersAndRetrievesSanitizedDetail() throws Exception {
        String admin =
                tokenFor(
                        "audit-admin@example.com",
                        com.chalchitraghar.modules.users.enums.Role.ADMIN);
        var first =
                append(
                        AuditAction.LOGIN_SUCCEEDED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        7L,
                        "Person@Example.com");
        var second = append(AuditAction.ACCESS_DENIED, LocalDateTime.now(clock), null, null);

        mockMvc.perform(
                        get("/api/admin/audit-logs")
                                .header("Authorization", bearer(admin))
                                .param("action", "ACCESS_DENIED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(second.id()))
                .andExpect(jsonPath("$.data.content[0].beforeValues").doesNotExist());
        mockMvc.perform(
                        get("/api/admin/audit-logs/{id}", first.id())
                                .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.actorEmail").value("pe***@example.com"))
                .andExpect(jsonPath("$.data.beforeValues.status").value("OLD"))
                .andExpect(jsonPath("$.data.metadata.source").value("integration-test"));
    }

    @Test
    void stableOrderUsesOccurredAtThenId() throws Exception {
        String admin =
                tokenFor(
                        "order-admin@example.com",
                        com.chalchitraghar.modules.users.enums.Role.ADMIN);
        LocalDateTime time = LocalDateTime.now(clock);
        var olderId = append(AuditAction.MOVIE_CREATED, time, null, null);
        var newerId = append(AuditAction.MOVIE_UPDATED, time, null, null);
        mockMvc.perform(get("/api/admin/audit-logs").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(newerId.id()))
                .andExpect(jsonPath("$.data.content[1].id").value(olderId.id()));
    }

    @Test
    void listAndDetailAreAdminOnly() throws Exception {
        var row = append(AuditAction.LOGIN_FAILED, null, null, null);
        String customer =
                tokenFor(
                        "audit-customer@example.com",
                        com.chalchitraghar.modules.users.enums.Role.CUSTOMER);
        String staff =
                tokenFor(
                        "audit-staff@example.com",
                        com.chalchitraghar.modules.users.enums.Role.STAFF);
        mockMvc.perform(get("/api/admin/audit-logs")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/audit-logs").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/audit-logs").header("Authorization", bearer(customer)))
                .andExpect(status().isForbidden());
        mockMvc.perform(
                        get("/api/admin/audit-logs/{id}", row.id())
                                .header("Authorization", bearer(staff)))
                .andExpect(status().isForbidden());
    }

    @Test
    void badFiltersPaginationAndMissingDetailUseStandardErrors() throws Exception {
        String admin =
                tokenFor(
                        "errors-admin@example.com",
                        com.chalchitraghar.modules.users.enums.Role.ADMIN);
        mockMvc.perform(
                        get("/api/admin/audit-logs")
                                .header("Authorization", bearer(admin))
                                .param("size", "101"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(
                        get("/api/admin/audit-logs")
                                .header("Authorization", bearer(admin))
                                .param("action", "NOPE"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(
                        get("/api/admin/audit-logs")
                                .header("Authorization", bearer(admin))
                                .param("occurredFrom", "2026-07-14T00:00:00")
                                .param("occurredTo", "2026-07-13T00:00:00"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/audit-logs/999999").header("Authorization", bearer(admin)))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                        put("/api/admin/audit-logs/1")
                                .header("Authorization", bearer(admin))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void appendUsesClockAndPermitsActorsWithoutUsers() {
        LocalDateTime before = LocalDateTime.now(clock);
        var row = append(AuditAction.SHOW_STATUS_RECONCILED, null, null, null);
        LocalDateTime after = LocalDateTime.now(clock);
        var stored = auditLogRepository.findById(row.id()).orElseThrow();
        assertThat(stored.getActorUserId()).isNull();
        assertThat(stored.getOccurredAt()).isBetween(before, after);
        assertThat(stored.getCreatedAt()).isBetween(before, after);
    }

    @Test
    void rejectsSensitiveNestedCaseInsensitiveAndOversizedSnapshots() {
        for (String key :
                new String[] {
                    "password",
                    "passwordHash",
                    "otp",
                    "resetToken",
                    "jwt",
                    "refreshToken",
                    "googleToken",
                    "qrToken",
                    "smtpPassword",
                    "paymentSecret",
                    "requestBody"
                })
            assertThatThrownBy(() -> appendWith(Map.of(key, "secret")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("forbidden key");
        assertThatThrownBy(() -> appendWith(Map.of("oldValue", Map.of("PaSsWoRd", "secret"))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> appendWith(Map.of("oldValue", "x".repeat(17000))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> appendWith(Map.of("notApproved", "value")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("allowlisted");
    }

    private com.chalchitraghar.modules.audit.dto.response.AdminAuditLogDetailResponse append(
            AuditAction action, LocalDateTime occurredAt, Long userId, String email) {
        return auditLogService.append(
                new CreateAuditLogCommand(
                        occurredAt,
                        userId,
                        email,
                        userId == null ? null : "ADMIN",
                        userId == null ? AuditActorType.SYSTEM : AuditActorType.USER,
                        action,
                        AuditCategory.SECURITY,
                        AuditSeverity.INFO,
                        "SYSTEM",
                        12L,
                        "safe-ref",
                        AuditResult.SUCCESS,
                        "token=must-not-leak\nline",
                        "req-1",
                        "corr-1",
                        Map.of("status", "OLD"),
                        Map.of("status", "NEW"),
                        Map.of("source", "integration-test")));
    }

    private void appendWith(Map<String, ?> snapshot) {
        auditLogService.append(
                new CreateAuditLogCommand(
                        null,
                        null,
                        null,
                        null,
                        AuditActorType.SYSTEM,
                        AuditAction.LOGIN_FAILED,
                        AuditCategory.SECURITY,
                        AuditSeverity.WARNING,
                        "SYSTEM",
                        null,
                        null,
                        AuditResult.FAILURE,
                        null,
                        null,
                        null,
                        snapshot,
                        null,
                        null));
    }
}
