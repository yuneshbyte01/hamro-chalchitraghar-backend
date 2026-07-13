package com.chalchitraghar.modules.notifications.config;

import jakarta.validation.constraints.*;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.notifications.reminders")
public class NotificationReminderProperties {
    private boolean enabled;
    @NotNull private Duration showBefore = Duration.ofHours(2);
    @NotNull private Duration scanInterval = Duration.ofMinutes(5);

    @Min(1)
    private int batchSize = 100;
}
