package com.chalchitraghar.shared.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class ExecutorMetrics {
    private final Counter rejected;

    public ExecutorMetrics(MeterRegistry registry) {
        rejected =
                Counter.builder("chalchitraghar.executor.rejected")
                        .description("Tasks rejected by a bounded application executor")
                        .tag("executor", "notification_email")
                        .register(registry);
    }

    public void rejected() {
        rejected.increment();
    }
}
