package com.chalchitraghar.modules.notifications.service;

import com.chalchitraghar.modules.notifications.config.NotificationEmailProperties;
import com.chalchitraghar.shared.observability.ExecutorMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationEmailDispatchLauncher {
    private final NotificationEmailDispatcher dispatcher;
    private final NotificationEmailProperties properties;
    private final TaskExecutor executor;
    private final ExecutorMetrics executorMetrics;

    public NotificationEmailDispatchLauncher(
            NotificationEmailDispatcher dispatcher,
            NotificationEmailProperties properties,
            @Qualifier("notificationEmailExecutor") TaskExecutor executor,
            ExecutorMetrics executorMetrics) {
        this.dispatcher = dispatcher;
        this.properties = properties;
        this.executor = executor;
        this.executorMetrics = executorMetrics;
    }

    public void submit(Long deliveryId) {
        if (!properties.getAsync().isEnabled()) {
            dispatcher.dispatch(deliveryId);
            return;
        }
        try {
            executor.execute(() -> dispatcher.dispatch(deliveryId));
        } catch (RuntimeException rejected) {
            executorMetrics.rejected();
            log.warn(
                    "Notification email dispatch submission rejected deliveryId={} reason={}",
                    deliveryId,
                    rejected.getClass().getSimpleName());
        }
    }
}
