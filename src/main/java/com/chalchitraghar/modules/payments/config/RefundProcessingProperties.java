package com.chalchitraghar.modules.payments.config;

import com.chalchitraghar.modules.payments.enums.RefundMethod;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("app.refunds.processing")
public class RefundProcessingProperties {
    private boolean enabled = true;
    private boolean autoProcessEnabled = false;
    private RefundMethod defaultMethod = RefundMethod.MANUAL;
    private int maxAttempts = 3;
    private Duration initialRetryDelay = Duration.ofMinutes(1);
    private double retryMultiplier = 2.0;
    private Duration maxRetryDelay = Duration.ofHours(1);
    private int retryBatchSize = 50;
    private Duration retryInterval = Duration.ofMinutes(1);
    private Duration processingTimeout = Duration.ofMinutes(5);
    private int claimBatchSize = 50;
    private String workerId = "refund-worker";
}
