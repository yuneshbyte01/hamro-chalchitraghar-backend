package com.chalchitraghar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.chalchitraghar.modules.audit.enums.*;
import com.chalchitraghar.modules.users.enums.Role;
import com.chalchitraghar.shared.audit.RequestAuditContextHolder;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class RequestAuditContextIntegrationTest extends AbstractIntegrationTest {
    @Test
    void acceptsSafeIdsGeneratesMissingIdsAndAlwaysReturnsHeaders() throws Exception {
        mockMvc.perform(
                        get("/api/public/health")
                                .header("X-Request-ID", "client.req-1")
                                .header("X-Correlation-ID", "flow:42"))
                .andExpect(header().string("X-Request-ID", "client.req-1"))
                .andExpect(header().string("X-Correlation-ID", "flow:42"));
        var response =
                mockMvc.perform(get("/api/public/health").header("X-Request-ID", "bad value!"))
                        .andReturn()
                        .getResponse();
        assertThat(response.getHeader("X-Request-ID")).matches("[0-9a-f-]{36}");
        assertThat(response.getHeader("X-Correlation-ID"))
                .isEqualTo(response.getHeader("X-Request-ID"));
        assertThat(RequestAuditContextHolder.current()).isEmpty();
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    void failedLoginPersistsSanitizedRequestContextWithoutQueryOrBody() throws Exception {
        saveUser("context-login@example.com", Role.CUSTOMER);
        mockMvc.perform(
                        post("/api/auth/login?password=must-not-appear")
                                .header("X-Request-ID", "login-request")
                                .header("X-Correlation-ID", "login-flow")
                                .header("User-Agent", "Audit Client 1.0")
                                .contentType("application/json")
                                .content(
                                        json(
                                                java.util.Map.of(
                                                        "email",
                                                        "context-login@example.com",
                                                        "password",
                                                        "wrong"))))
                .andExpect(status().isUnauthorized());
        var audit =
                auditLogRepository.findAllByActionOrderByIdAsc(AuditAction.LOGIN_FAILED).getFirst();
        assertThat(audit.getRequestId()).isEqualTo("login-request");
        assertThat(audit.getCorrelationId()).isEqualTo("login-flow");
        assertThat(audit.getHttpMethod()).isEqualTo("POST");
        assertThat(audit.getRequestPath()).isEqualTo("/api/auth/login");
        assertThat(audit.getRequestPath()).doesNotContain("password");
        assertThat(audit.getUserAgent()).isEqualTo("Audit Client 1.0");
        assertThat(audit.getIpAddress()).isNull();
    }

    @Test
    void invalidJwtAndAccessDeniedAreAuditedOnceWithStandardResponses() throws Exception {
        mockMvc.perform(
                        get("/api/admin/audit-logs")
                                .header("Authorization", "Bearer malformed")
                                .header("X-Request-ID", "jwt-request"))
                .andExpect(status().isUnauthorized());
        assertThat(auditLogRepository.countByAction(AuditAction.INVALID_JWT_REJECTED)).isEqualTo(1);
        var jwtAudit =
                auditLogRepository
                        .findAllByActionOrderByIdAsc(AuditAction.INVALID_JWT_REJECTED)
                        .getFirst();
        assertThat(jwtAudit.getFailureReason()).isEqualTo("TOKEN_INVALID");
        assertThat(jwtAudit.getMetadata()).doesNotContain("malformed");

        String customer = tokenFor("denied-context@example.com", Role.CUSTOMER);
        mockMvc.perform(
                        get("/api/admin/audit-logs")
                                .header("Authorization", bearer(customer))
                                .header("X-Request-ID", "denied-request"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
        var denied = auditLogRepository.findAllByActionOrderByIdAsc(AuditAction.ACCESS_DENIED);
        assertThat(denied).hasSize(1);
        assertThat(denied.getFirst().getRequestId()).isEqualTo("denied-request");
        assertThat(denied.getFirst().getActorUserId()).isNotNull();
    }
}
