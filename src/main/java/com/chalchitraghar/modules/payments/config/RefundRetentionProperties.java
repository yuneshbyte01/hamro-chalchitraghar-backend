package com.chalchitraghar.modules.payments.config;

import jakarta.validation.constraints.*;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties("app.refunds.retention")
public class RefundRetentionProperties {
    private boolean enabled = false;

    @Pattern(regexp = "ANONYMIZE")
    private String mode = "ANONYMIZE";

    @Min(1)
    private int refundDays = 2555;

    @Min(1)
    private int attemptDays = 2555;

    @Min(1)
    private int batchSize = 100;

    @NotNull private Duration interval = Duration.ofHours(24);
}
