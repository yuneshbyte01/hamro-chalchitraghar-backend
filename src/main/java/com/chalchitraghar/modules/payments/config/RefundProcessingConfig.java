package com.chalchitraghar.modules.payments.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    RefundProcessingProperties.class,
    RefundReportProperties.class,
    RefundRetentionProperties.class,
    RefundConsistencyProperties.class
})
public class RefundProcessingConfig {}
