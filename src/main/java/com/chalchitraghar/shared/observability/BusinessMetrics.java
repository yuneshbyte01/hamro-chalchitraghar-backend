package com.chalchitraghar.shared.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Locale;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BusinessMetrics {
    private final MeterRegistry registry;

    public <T> T observe(BusinessMetric metric, Enum<?> operation, Supplier<T> action) {
        Timer.Sample sample = Timer.start(registry);
        MetricOutcome outcome = MetricOutcome.SUCCESS;
        try {
            return action.get();
        } catch (RuntimeException failure) {
            outcome = MetricOutcome.from(failure);
            throw failure;
        } finally {
            record(metric, operation, outcome, sample);
        }
    }

    public void observe(BusinessMetric metric, Enum<?> operation, Runnable action) {
        observe(
                metric,
                operation,
                () -> {
                    action.run();
                    return null;
                });
    }

    public void increment(BusinessMetric metric, Enum<?> operation, MetricOutcome outcome) {
        registry.counter(metric.counter(), "operation", tag(operation), "outcome", outcome.tag())
                .increment();
    }

    private void record(
            BusinessMetric metric, Enum<?> operation, MetricOutcome outcome, Timer.Sample sample) {
        String operationTag = tag(operation);
        registry.counter(metric.counter(), "operation", operationTag, "outcome", outcome.tag())
                .increment();
        sample.stop(
                registry.timer(
                        metric.timer(), "operation", operationTag, "outcome", outcome.tag()));
    }

    private String tag(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }
}
