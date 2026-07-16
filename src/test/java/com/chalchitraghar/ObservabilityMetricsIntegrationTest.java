package com.chalchitraghar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chalchitraghar.modules.users.enums.Role;
import com.chalchitraghar.shared.observability.BusinessMetric;
import com.chalchitraghar.shared.observability.BusinessMetrics;
import com.chalchitraghar.shared.observability.BusinessOperation;
import com.chalchitraghar.shared.observability.JobName;
import com.chalchitraghar.shared.observability.ScheduledJobObserver;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

class ObservabilityMetricsIntegrationTest extends AbstractIntegrationTest {
    private static final Set<String> FORBIDDEN_TAGS =
            Set.of(
                    "userId",
                    "email",
                    "bookingReference",
                    "paymentReference",
                    "refundReference",
                    "ticketReference",
                    "notificationId",
                    "auditId",
                    "requestId",
                    "correlationId",
                    "rawPath");

    @Autowired private MeterRegistry registry;
    @Autowired private BusinessMetrics businessMetrics;
    @Autowired private ScheduledJobObserver jobs;

    @Test
    void businessMetersReuseBoundedSeriesAndRecordFailures() {
        double bookingBefore =
                counterValue(
                        "chalchitraghar.booking.operations",
                        "operation",
                        "create",
                        "outcome",
                        "success");
        for (BusinessMetric metric : BusinessMetric.values()) {
            businessMetrics.observe(metric, BusinessOperation.CREATE, () -> "ok");
        }
        int afterFirst = customMeters().size();
        for (BusinessMetric metric : BusinessMetric.values()) {
            businessMetrics.observe(metric, BusinessOperation.CREATE, () -> "ok-again");
        }
        assertThat(customMeters().size()).isEqualTo(afterFirst);
        assertThatThrownBy(
                        () ->
                                businessMetrics.observe(
                                        BusinessMetric.BOOKING,
                                        BusinessOperation.CONFIRM,
                                        () -> {
                                            throw new IllegalStateException("controlled");
                                        }))
                .isInstanceOf(IllegalStateException.class);

        assertThat(
                        registry.get("chalchitraghar.booking.operations")
                                .tag("operation", "create")
                                .tag("outcome", "success")
                                .counter()
                                .count())
                .isEqualTo(bookingBefore + 2);
        assertThat(
                        registry.get("chalchitraghar.booking.duration")
                                .tag("operation", "create")
                                .tag("outcome", "success")
                                .timer()
                                .count())
                .isGreaterThanOrEqualTo(2);
        assertThat(
                        registry.get("chalchitraghar.booking.operations")
                                .tag("operation", "confirm")
                                .tag("outcome", "failure")
                                .counter()
                                .count())
                .isGreaterThanOrEqualTo(1);
    }

    @Test
    void scheduledJobMetricsRecordSuccessFailureProcessedAndClockTimestamp() {
        long before = clock.instant().getEpochSecond();
        double executionsBefore =
                counterValue(
                        "chalchitraghar.job.executions",
                        "job",
                        "booking_expiry",
                        "outcome",
                        "success");
        double processedBefore =
                counterValue("chalchitraghar.job.processed", "job", "booking_expiry");
        double failuresBefore =
                counterValue("chalchitraghar.job.failures", "job", "payment_reconciliation");
        assertThat(jobs.observe(JobName.BOOKING_EXPIRY, () -> 3)).isEqualTo(3);
        assertThatThrownBy(
                        () ->
                                jobs.observe(
                                        JobName.PAYMENT_RECONCILIATION,
                                        () -> {
                                            throw new IllegalStateException("controlled");
                                        }))
                .isInstanceOf(IllegalStateException.class);

        assertThat(
                        registry.get("chalchitraghar.job.executions")
                                .tag("job", "booking_expiry")
                                .tag("outcome", "success")
                                .counter()
                                .count())
                .isEqualTo(executionsBefore + 1);
        assertThat(
                        registry.get("chalchitraghar.job.processed")
                                .tag("job", "booking_expiry")
                                .counter()
                                .count())
                .isEqualTo(processedBefore + 3);
        assertThat(
                        registry.get("chalchitraghar.job.failures")
                                .tag("job", "payment_reconciliation")
                                .counter()
                                .count())
                .isEqualTo(failuresBefore + 1);
        assertThat(
                        registry.get("chalchitraghar.job.last_success.timestamp")
                                .tag("job", "booking_expiry")
                                .gauge()
                                .value())
                .isBetween((double) before, (double) clock.instant().getEpochSecond());
    }

    @Test
    void executorAndCustomMetricsUseOnlyGovernedTags() {
        assertThat(registry.find("executor.active").tag("name", "notification_email").gauges())
                .isNotEmpty();
        assertThat(registry.find("executor.queued").tag("name", "notification_email").gauges())
                .isNotEmpty();
        assertThat(registry.find("chalchitraghar.executor.rejected").counters()).hasSize(1);

        for (Meter meter : customMeters()) {
            assertThat(meter.getId().getTags())
                    .allSatisfy(tag -> assertThat(FORBIDDEN_TAGS).doesNotContain(tag.getKey()));
        }
    }

    @Test
    void adminPrometheusScrapeContainsRepresentativeCustomMetrics() throws Exception {
        businessMetrics.observe(BusinessMetric.BOOKING, BusinessOperation.CREATE, () -> "ok");
        businessMetrics.observe(BusinessMetric.PAYMENT, BusinessOperation.INITIATE, () -> "ok");
        businessMetrics.observe(
                BusinessMetric.TICKET_VALIDATION, BusinessOperation.VALIDATE, () -> "ok");
        jobs.observe(JobName.NOTIFICATION_RETENTION, () -> 0);
        String admin = tokenFor("custom-metrics-admin@example.com", Role.ADMIN);

        MvcResult result =
                mockMvc.perform(get("/actuator/prometheus").header("Authorization", bearer(admin)))
                        .andExpect(status().isOk())
                        .andReturn();

        assertThat(result.getResponse().getContentAsString())
                .contains(
                        "chalchitraghar_booking_operations_total",
                        "chalchitraghar_payment_operations_total",
                        "chalchitraghar_ticket_validation_total",
                        "chalchitraghar_job_executions_total")
                .doesNotContain("custom-metrics-admin@example.com");
    }

    private java.util.List<Meter> customMeters() {
        return registry.getMeters().stream()
                .filter(meter -> meter.getId().getName().startsWith("chalchitraghar."))
                .toList();
    }

    private double counterValue(String name, String... tags) {
        var search = registry.find(name);
        for (int i = 0; i < tags.length; i += 2) search = search.tag(tags[i], tags[i + 1]);
        var counter = search.counter();
        return counter == null ? 0 : counter.count();
    }
}
