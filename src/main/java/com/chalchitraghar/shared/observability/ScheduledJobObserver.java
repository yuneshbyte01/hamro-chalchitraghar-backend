package com.chalchitraghar.shared.observability;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class ScheduledJobObserver {
    private static final Logger log = LoggerFactory.getLogger(ScheduledJobObserver.class);
    private static final String EXECUTIONS = "chalchitraghar.job.executions";
    private static final String DURATION = "chalchitraghar.job.duration";
    private static final String PROCESSED = "chalchitraghar.job.processed";
    private static final String FAILURES = "chalchitraghar.job.failures";
    private static final String LAST_SUCCESS = "chalchitraghar.job.last_success.timestamp";

    private final MeterRegistry registry;
    private final Clock clock;
    private final Map<JobName, AtomicLong> lastSuccess = new EnumMap<>(JobName.class);

    public ScheduledJobObserver(MeterRegistry registry, Clock clock) {
        this.registry = registry;
        this.clock = clock;
        for (JobName job : JobName.values()) {
            AtomicLong timestamp = new AtomicLong();
            lastSuccess.put(job, timestamp);
            Gauge.builder(LAST_SUCCESS, timestamp, AtomicLong::get)
                    .description("Epoch-second timestamp of the last successful in-process job run")
                    .tag("job", job.tag())
                    .register(registry);
        }
    }

    public int observe(JobName job, IntSupplier action) {
        Map<String, String> previousContext = MDC.getCopyOfContextMap();
        String jobRunId = UUID.randomUUID().toString();
        MDC.clear();
        MDC.put("job", job.tag());
        MDC.put("jobRunId", jobRunId);
        MDC.put("correlationId", jobRunId);
        MDC.put("jobStartedAt", clock.instant().toString());
        Timer.Sample sample = Timer.start(registry);
        MetricOutcome outcome = MetricOutcome.SUCCESS;
        int processed = 0;
        try {
            processed = action.getAsInt();
            registry.counter(PROCESSED, "job", job.tag()).increment(processed);
            lastSuccess.get(job).set(clock.instant().getEpochSecond());
            return processed;
        } catch (RuntimeException failure) {
            outcome = MetricOutcome.FAILURE;
            registry.counter(FAILURES, "job", job.tag()).increment();
            long durationNanos =
                    sample.stop(
                            registry.timer(DURATION, "job", job.tag(), "outcome", outcome.tag()));
            log.atError()
                    .setCause(failure)
                    .addKeyValue("event", "job.failed")
                    .addKeyValue("outcome", outcome.tag())
                    .addKeyValue("processedCount", processed)
                    .addKeyValue("failureCount", 1)
                    .addKeyValue("durationMs", TimeUnit.NANOSECONDS.toMillis(durationNanos))
                    .addKeyValue("exceptionType", failure.getClass().getSimpleName())
                    .log("Scheduled job failed");
            throw failure;
        } finally {
            registry.counter(EXECUTIONS, "job", job.tag(), "outcome", outcome.tag()).increment();
            if (outcome == MetricOutcome.SUCCESS) {
                long durationNanos =
                        sample.stop(
                                registry.timer(
                                        DURATION, "job", job.tag(), "outcome", outcome.tag()));
                var event =
                        processed == 0
                                ? log.atDebug().addKeyValue("event", "job.completed")
                                : log.atInfo().addKeyValue("event", "job.completed");
                event.addKeyValue("outcome", outcome.tag())
                        .addKeyValue("processedCount", processed)
                        .addKeyValue("failureCount", 0)
                        .addKeyValue("durationMs", TimeUnit.NANOSECONDS.toMillis(durationNanos))
                        .log("Scheduled job completed");
            }
            MDC.clear();
            if (previousContext != null) {
                MDC.setContextMap(previousContext);
            }
        }
    }

    /**
     * Runs a job with the same diagnostics while preserving jobs that historically suppressed
     * failures.
     */
    public void observeAndSuppress(JobName job, IntSupplier action) {
        try {
            observe(job, action);
        } catch (RuntimeException ignored) {
            // The observer already emitted the single failure log; persisted work remains
            // retryable.
        }
    }
}
