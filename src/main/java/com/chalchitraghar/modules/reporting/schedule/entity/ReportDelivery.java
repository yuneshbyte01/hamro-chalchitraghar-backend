package com.chalchitraghar.modules.reporting.schedule.entity;

import com.chalchitraghar.modules.reporting.enums.*;
import com.chalchitraghar.shared.GenericEntity;
import jakarta.persistence.*;
import java.time.*;
import lombok.*;

@Entity
@Table(
        name = "report_deliveries",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_report_deliveries_idempotency",
                        columnNames = "idempotency_key"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDelivery extends GenericEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scheduled_report_id", nullable = false)
    private ScheduledReport scheduledReport;

    @Column(nullable = false)
    private LocalDate periodStart;

    @Column(nullable = false)
    private LocalDate periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReportExportFormat format;

    @Column(nullable = false, length = 320)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportDeliveryStatus status;

    @Column(nullable = false)
    private int attemptCount;

    private LocalDateTime lastAttemptAt;
    private LocalDateTime nextAttemptAt;
    private LocalDateTime sentAt;

    @Column(length = 500)
    private String failureReason;

    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    @Column(nullable = false, length = 180)
    private String fileName;

    private Long fileSizeBytes;

    @Override
    @PrePersist
    protected void onCreate() {
        if (getCreatedAt() == null || getUpdatedAt() == null)
            throw new IllegalStateException("Delivery audit timestamps are required");
    }

    @Override
    @PreUpdate
    protected void onUpdate() {
        if (getUpdatedAt() == null)
            throw new IllegalStateException("Delivery updatedAt is required");
    }
}
