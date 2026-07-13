package com.chalchitraghar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chalchitraghar.modules.audit.enums.*;
import com.chalchitraghar.modules.audit.event.*;
import com.chalchitraghar.modules.audit.listener.AuditEventListener;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AuditEventIntegrationTest extends AbstractIntegrationTest {
    @Autowired AuditEventListener listener;

    @Test
    void registrationAndSuccessfulLoginAreRecordedAfterCommitWithoutSecrets() throws Exception {
        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType("application/json")
                                .content(
                                        json(
                                                Map.of(
                                                        "name",
                                                        "Audit User",
                                                        "email",
                                                        "events@example.com",
                                                        "password",
                                                        "Password123!"))))
                .andExpect(status().isCreated());
        assertThat(auditLogRepository.countByAction(AuditAction.USER_REGISTERED)).isEqualTo(1);

        for (int i = 0; i < 2; i++)
            mockMvc.perform(
                            post("/api/auth/login")
                                    .contentType("application/json")
                                    .content(
                                            json(
                                                    Map.of(
                                                            "email",
                                                            "events@example.com",
                                                            "password",
                                                            "Password123!"))))
                    .andExpect(status().isOk());
        var attempts = auditLogRepository.findAllByActionOrderByIdAsc(AuditAction.LOGIN_SUCCEEDED);
        assertThat(attempts).hasSize(2);
        assertThat(attempts).extracting(a -> a.getEventId()).doesNotHaveDuplicates();
        assertThat(attempts)
                .allSatisfy(
                        a -> {
                            assertThat(a.getActorType()).isEqualTo(AuditActorType.USER);
                            assertThat(String.valueOf(a.getMetadata()))
                                    .doesNotContainIgnoringCase("password", "token");
                        });
    }

    @Test
    void genuineFailedLoginAttemptsRemainSeparateAndContainNoCredential() throws Exception {
        saveUser("failed-events@example.com", com.chalchitraghar.modules.users.enums.Role.CUSTOMER);
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(
                            post("/api/auth/login")
                                    .contentType("application/json")
                                    .content(
                                            json(
                                                    Map.of(
                                                            "email",
                                                            "failed-events@example.com",
                                                            "password",
                                                            "Wrong123!"))))
                    .andExpect(status().isUnauthorized());
        }
        var attempts = auditLogRepository.findAllByActionOrderByIdAsc(AuditAction.LOGIN_FAILED);
        assertThat(attempts).hasSize(2);
        assertThat(attempts).extracting(a -> a.getEventId()).doesNotHaveDuplicates();
        assertThat(attempts)
                .allSatisfy(
                        a -> {
                            assertThat(a.getFailureReason()).isEqualTo("INVALID_CREDENTIALS");
                            assertThat(a.getBeforeValues()).isNull();
                            assertThat(a.getAfterValues()).isNull();
                        });
    }

    @Test
    void duplicateDeliveryCreatesOneRow() {
        AuditEvent event =
                new AuditEvent(
                        "TEST-STABLE-EVENT",
                        LocalDateTime.now(clock),
                        new AuditActor(AuditActorType.SYSTEM, null, null, "SYSTEM"),
                        AuditAction.SHOW_STATUS_RECONCILED,
                        AuditCategory.SYSTEM,
                        AuditSeverity.INFO,
                        AuditResult.SUCCESS,
                        "SYSTEM",
                        null,
                        "test",
                        null,
                        null,
                        Map.of("status", "COMPLETED"),
                        Map.of("source", "integration-test"));
        listener.persist(event);
        listener.persist(event);
        assertThat(auditLogRepository.countByAction(AuditAction.SHOW_STATUS_RECONCILED))
                .isEqualTo(1);
    }
}
