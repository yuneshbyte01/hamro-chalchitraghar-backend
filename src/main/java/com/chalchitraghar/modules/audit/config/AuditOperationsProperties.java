package com.chalchitraghar.modules.audit.config;

import com.chalchitraghar.modules.audit.enums.AuditReportBucket;
import com.chalchitraghar.modules.audit.enums.AuditRetentionMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.audit")
public class AuditOperationsProperties {
    @Valid private Export export = new Export();
    @Valid private Retention retention = new Retention();
    @Valid private Reports reports = new Reports();
    @Valid private Integrity integrity = new Integrity();

    @Getter
    @Setter
    public static class Export {
        private boolean enabled = true;

        @Min(1)
        @Max(100000)
        private int maxRows = 10000;

        @Min(1)
        @Max(366)
        private int maxRangeDays = 31;
    }

    @Getter
    @Setter
    public static class Retention {
        private boolean enabled;
        @NotNull private AuditRetentionMode mode = AuditRetentionMode.ANONYMIZE;

        @Min(1)
        private int defaultDays = 365;

        @Min(1)
        private int securityDays = 730;

        @Min(1)
        private int paymentDays = 730;

        @Min(1)
        private int highSeverityDays = 730;

        @Min(1)
        @Max(1000)
        private int batchSize = 100;

        @NotNull private Duration interval = Duration.ofHours(24);
    }

    @Getter
    @Setter
    public static class Reports {
        @Min(1)
        @Max(366)
        private int maxRangeDays = 31;

        @NotNull private AuditReportBucket defaultBucket = AuditReportBucket.DAY;
    }

    @Getter
    @Setter
    public static class Integrity {
        private boolean enabled = true;

        @Min(1)
        @Max(10000)
        private int batchSize = 500;

        @NotNull private Duration interval = Duration.ofHours(24);
    }
}
