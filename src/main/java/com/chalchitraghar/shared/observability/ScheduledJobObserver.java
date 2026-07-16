package com.chalchitraghar.shared.observability;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntSupplier;
import org.springframework.stereotype.Component;

@Component
public class ScheduledJobObserver {
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
        Timer.Sample sample = Timer.start(registry);
        MetricOutcome outcome = MetricOutcome.SUCCESS;
        try {
            int processed = action.getAsInt();
            registry.counter(PROCESSED, "job", job.tag()).increment(processed);
            lastSuccess.get(job).set(clock.instant().getEpochSecond());
            return processed;
        } catch (RuntimeException failure) {
            outcome = MetricOutcome.FAILURE;
            registry.counter(FAILURES, "job", job.tag()).increment();
            throw failure;
        } finally {
            registry.counter(EXECUTIONS, "job", job.tag(), "outcome", outcome.tag()).increment();
            sample.stop(registry.timer(DURATION, "job", job.tag(), "outcome", outcome.tag()));
        }
    }
}
