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
@ConfigurationProperties(prefix = "app.notifications.retention")
public class NotificationRetentionProperties {
    private boolean enabled;

    @Min(1)
    private int notificationDays = 365;

    @Min(1)
    private int deliveryDays = 180;

    @Min(1)
    private int batchSize = 100;

    @NotNull private Duration interval = Duration.ofHours(24);
}
