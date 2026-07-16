package com.chalchitraghar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chalchitraghar.modules.users.enums.Role;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MvcResult;

class ObservabilityApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MeterRegistry meterRegistry;
    @Autowired private Environment environment;
    @Autowired private ThreadPoolTaskExecutor notificationEmailExecutor;

    @Test
    void compatibilityHealthRemainsStable() throws Exception {
        mockMvc.perform(get("/api/public/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    void publicHealthAndProbesUseStatusOnlyActuatorResponses() throws Exception {
        for (String endpoint :
                new String[] {
                    "/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness"
                }) {
            mockMvc.perform(get(endpoint))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.components").doesNotExist())
                    .andExpect(jsonPath("$.success").doesNotExist());
        }

        assertThat(environment.getProperty("management.endpoint.health.group.liveness.include"))
                .isEqualTo("livenessState,ping");
        assertThat(environment.getProperty("management.endpoint.health.group.readiness.include"))
                .isEqualTo("readinessState,db");
    }

    @Test
    void infoIsAdminOnlyAndContainsSafeMetadata() throws Exception {
        String customer = tokenFor("info-customer@example.com", Role.CUSTOMER);
        String staff = tokenFor("info-staff@example.com", Role.STAFF);
        String admin = tokenFor("info-admin@example.com", Role.ADMIN);

        mockMvc.perform(get("/actuator/info")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/info").header("Authorization", bearer(customer)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/actuator/info").header("Authorization", bearer(staff)))
                .andExpect(status().isForbidden());

        MvcResult result =
                mockMvc.perform(get("/actuator/info").header("Authorization", bearer(admin)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.app.name").value("Hamro Chalchitraghar"))
                        .andExpect(
                                jsonPath("$.app.description").value("Movie ticket booking backend"))
                        .andExpect(jsonPath("$.app.version").value("local"))
                        .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body)
                .doesNotContain("jwt", "secret", "password", "smtp", "DB_URL", "client-secret");
    }

    @Test
    void prometheusIsAdminOnlyAndExportsBuiltInMetrics() throws Exception {
        String customer = tokenFor("metrics-customer@example.com", Role.CUSTOMER);
        String staff = tokenFor("metrics-staff@example.com", Role.STAFF);
        String admin = tokenFor("metrics-admin@example.com", Role.ADMIN);

        mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/prometheus").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/prometheus").header("Authorization", bearer(customer)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/actuator/prometheus").header("Authorization", bearer(staff)))
                .andExpect(status().isForbidden());

        MvcResult result =
                mockMvc.perform(get("/actuator/prometheus").header("Authorization", bearer(admin)))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith("text/plain"))
                        .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body)
                .contains("jvm_memory_used_bytes", "process_uptime_seconds", "http_server_requests")
                .doesNotContain("test-secret-key-for-jwt-signing", "smtp.gmail.com");
        assertThat(meterRegistry.find("http.server.requests").meters()).isNotEmpty();
        assertThat(meterRegistry.find("jvm.memory.used").meters()).isNotEmpty();
        assertThat(meterRegistry.find("process.uptime").meters()).isNotEmpty();
        assertThat(meterRegistry.find("hikaricp.connections").meters()).isNotEmpty();
    }

    @Test
    void sensitiveActuatorEndpointsAreNotExposed() throws Exception {
        String admin = tokenFor("endpoint-admin@example.com", Role.ADMIN);
        for (String endpoint :
                new String[] {"env", "configprops", "beans", "mappings", "heapdump", "loggers"}) {
            mockMvc.perform(get("/actuator/" + endpoint).header("Authorization", bearer(admin)))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void gracefulShutdownAndExecutorDrainAreBounded() {
        assertThat(environment.getProperty("server.shutdown")).isEqualTo("graceful");
        assertThat(environment.getProperty("spring.lifecycle.timeout-per-shutdown-phase"))
                .isEqualTo("30s");
        assertThat(
                        ReflectionTestUtils.getField(
                                notificationEmailExecutor, "waitForTasksToCompleteOnShutdown"))
                .isEqualTo(true);
        assertThat(
                        ReflectionTestUtils.getField(
                                notificationEmailExecutor, "awaitTerminationMillis"))
                .isEqualTo(20_000L);
    }
}
