package com.chalchitraghar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chalchitraghar.modules.notifications.config.NotificationEmailProperties;
import com.chalchitraghar.modules.notifications.service.NotificationEmailDispatchLauncher;
import com.chalchitraghar.modules.notifications.service.NotificationEmailDispatcher;
import com.chalchitraghar.shared.observability.ExecutorMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;

class ExecutorRejectionMetricsTest {
    @Test
    void rejectedSubmissionIncrementsStableCounterWithoutDispatching() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        NotificationEmailProperties properties = mock(NotificationEmailProperties.class);
        NotificationEmailProperties.Async async = mock(NotificationEmailProperties.Async.class);
        when(properties.getAsync()).thenReturn(async);
        when(async.isEnabled()).thenReturn(true);
        TaskExecutor rejecting =
                task -> {
                    throw new org.springframework.core.task.TaskRejectedException("full");
                };
        NotificationEmailDispatchLauncher launcher =
                new NotificationEmailDispatchLauncher(
                        mock(NotificationEmailDispatcher.class),
                        properties,
                        rejecting,
                        new ExecutorMetrics(registry));

        launcher.submit(42L);

        assertThat(
                        registry.get("chalchitraghar.executor.rejected")
                                .tag("executor", "notification_email")
                                .counter()
                                .count())
                .isEqualTo(1);
    }
}
