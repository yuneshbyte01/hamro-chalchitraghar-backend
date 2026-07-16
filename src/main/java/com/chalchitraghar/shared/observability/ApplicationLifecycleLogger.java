package com.chalchitraghar.shared.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class ApplicationLifecycleLogger {
    private static final Logger log = LoggerFactory.getLogger(ApplicationLifecycleLogger.class);

    private final Environment environment;
    private final ObjectProvider<ThreadPoolTaskExecutor> notificationExecutor;
    private final String applicationVersion;
    private final String applicationEnvironment;
    private final String applicationTimeZone;

    public ApplicationLifecycleLogger(
            Environment environment,
            @Qualifier("notificationEmailExecutor")
                    ObjectProvider<ThreadPoolTaskExecutor> notificationExecutor,
            @Value("${info.app.version:local}") String applicationVersion,
            @Value("${management.metrics.tags.environment:local}") String applicationEnvironment,
            @Value("${app.time-zone:Asia/Kathmandu}") String applicationTimeZone) {
        this.environment = environment;
        this.notificationExecutor = notificationExecutor;
        this.applicationVersion = applicationVersion;
        this.applicationEnvironment = applicationEnvironment;
        this.applicationTimeZone = applicationTimeZone;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ready() {
        log.atInfo()
                .addKeyValue("event", "application.started")
                .addKeyValue("application", "hamro-chalchitraghar-backend")
                .addKeyValue("version", applicationVersion)
                .addKeyValue("environment", applicationEnvironment)
                .addKeyValue("profiles", String.join(",", environment.getActiveProfiles()))
                .addKeyValue("timeZone", applicationTimeZone)
                .log("Application ready");
    }

    @EventListener(ContextClosedEvent.class)
    public void shuttingDown() {
        ThreadPoolTaskExecutor executor = notificationExecutor.getIfAvailable();
        var event = log.atInfo().addKeyValue("event", "application.shutdown.started");
        if (executor != null) {
            event.addKeyValue("executor", "notification_email")
                    .addKeyValue("activeTasks", executor.getActiveCount())
                    .addKeyValue("queuedTasks", executor.getThreadPoolExecutor().getQueue().size());
        }
        event.log(
                "Application shutdown started; bounded graceful shutdown will drain available work");
    }
}
