package com.chalchitraghar.modules.notifications.config;

import com.chalchitraghar.shared.audit.AuditContextTaskDecorator;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@EnableConfigurationProperties({
    NotificationEmailProperties.class,
    NotificationReminderProperties.class,
    NotificationRetentionProperties.class
})
public class NotificationEmailConfig {
    @Bean(name = "notificationEmailExecutor")
    ThreadPoolTaskExecutor notificationEmailExecutor(NotificationEmailProperties properties) {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getExecutor().getCorePoolSize());
        executor.setMaxPoolSize(properties.getExecutor().getMaxPoolSize());
        executor.setQueueCapacity(properties.getExecutor().getQueueCapacity());
        executor.setKeepAliveSeconds(properties.getExecutor().getKeepAliveSeconds());
        executor.setThreadNamePrefix("notification-email-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setTaskDecorator(new AuditContextTaskDecorator());
        executor.initialize();
        return executor;
    }
}
