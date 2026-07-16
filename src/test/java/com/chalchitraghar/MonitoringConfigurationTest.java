package com.chalchitraghar;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MonitoringConfigurationTest {
    private static final Path MONITORING = Path.of("monitoring");
    private static final Set<String> FORBIDDEN =
            Set.of(
                    "userId",
                    "bookingReference",
                    "paymentReference",
                    "refundReference",
                    "ticketReference",
                    "requestId",
                    "correlationId",
                    "qrToken");

    private final ObjectMapper json = new ObjectMapper();

    @Test
    void prometheusUsesProtectedApplicationScrapeAndLoadsRules() throws IOException {
        String config = Files.readString(MONITORING.resolve("prometheus/prometheus.yml"));

        assertThat(config)
                .contains("job_name: hamro-chalchitraghar")
                .contains("metrics_path: /actuator/prometheus")
                .contains("basic_auth:")
                .contains("password_file: /run/secrets/prometheus_scrape_password")
                .contains("/etc/prometheus/rules/*.yml")
                .doesNotContain("Bearer ", "password:");
    }

    @Test
    void alertsHaveUniqueNamesSeverityDurationAndRunbooks() throws IOException {
        Set<String> alertNames = new HashSet<>();
        try (var files = Files.list(MONITORING.resolve("prometheus/rules"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".yml")).toList()) {
                String rule = Files.readString(file);
                assertThat(rule).contains("severity:", "runbook_url:", "for:");
                for (String line : rule.lines().toList()) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("- alert: ")) {
                        assertThat(alertNames.add(trimmed.substring(9))).isTrue();
                    }
                }
                assertThat(rule)
                        .doesNotContain("userId", "email=", "bookingReference", "requestId");
            }
        }
    }

    @Test
    void provisionedDashboardsHaveUniqueUidsPanelsDatasourceAndNoIdentifiers() throws IOException {
        Set<String> uids = new HashSet<>();
        try (var files = Files.list(MONITORING.resolve("grafana/dashboards"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".json")).toList()) {
                String source = Files.readString(file);
                JsonNode dashboard = json.readTree(source);
                assertThat(uids.add(dashboard.path("uid").asText())).isTrue();
                assertThat(dashboard.path("title").asText()).startsWith("Hamro Chalchitraghar");
                assertThat(dashboard.path("panels").isArray()).isTrue();
                assertThat(dashboard.path("panels").isEmpty()).isFalse();
                dashboard
                        .path("panels")
                        .forEach(
                                panel -> {
                                    assertThat(panel.path("title").asText()).isNotBlank();
                                    assertThat(panel.path("description").asText()).isNotBlank();
                                });
                assertThat(source).contains("hamro-prometheus");
                FORBIDDEN.forEach(forbidden -> assertThat(source).doesNotContain(forbidden));
            }
        }
        assertThat(uids).hasSize(5);
    }

    @Test
    void composePinsImagesDefinesHealthChecksVolumesAndSecretInjection() throws IOException {
        String compose = Files.readString(Path.of("docker-compose.observability.yml"));

        assertThat(compose)
                .contains("prom/prometheus:v3.5.0")
                .contains("grafana/grafana:12.0.2")
                .contains("prometheus_data:")
                .contains("grafana_data:")
                .contains("healthcheck:")
                .contains("prometheus_scrape_password")
                .contains("internal: true")
                .doesNotContain(":latest", "change-me-before-starting");
    }
}
