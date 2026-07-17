package com.chalchitraghar.modules.reporting.schedule.entity;

import com.chalchitraghar.modules.reporting.enums.*;
import com.chalchitraghar.shared.GenericEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "scheduled_reports")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledReport extends GenericEntity {
    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ScheduledReportType reportType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportScheduleFrequency scheduleFrequency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReportExportFormat deliveryFormat;

    @Column(nullable = false, length = 320)
    private String recipientEmail;

    @Column(length = 3)
    private String currency;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private boolean deleted;

    private LocalDateTime lastRunAt;
    private LocalDateTime nextRunAt;
    private Long createdByUserId;

    @Override
    @PrePersist
    protected void onCreate() {
        if (getCreatedAt() == null || getUpdatedAt() == null)
            throw new IllegalStateException("Schedule audit timestamps are required");
    }

    @Override
    @PreUpdate
    protected void onUpdate() {
        if (getUpdatedAt() == null)
            throw new IllegalStateException("Schedule updatedAt is required");
    }
}
