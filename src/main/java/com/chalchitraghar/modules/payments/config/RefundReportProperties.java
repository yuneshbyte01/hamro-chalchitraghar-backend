package com.chalchitraghar.modules.payments.config;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties("app.refunds.reports")
public class RefundReportProperties {
    @Min(1)
    private int maxRangeDays = 366;

    private String defaultBucket = "DAY";
}
