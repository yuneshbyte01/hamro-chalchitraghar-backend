package com.chalchitraghar.modules.notifications.entity;

import com.chalchitraghar.modules.notifications.enums.NotificationChannel;
import com.chalchitraghar.modules.notifications.enums.NotificationDeliveryStatus;
import com.chalchitraghar.shared.GenericEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
        name = "notification_deliveries",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_notification_deliveries_notification_channel",
                        columnNames = {"notification_id", "channel"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDelivery extends GenericEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationChannel channel;

    @Column(length = 320)
    private String recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationDeliveryStatus status;

    @Column(nullable = false)
    private int attemptCount;

    @Column(nullable = false)
    private int maxAttempts;

    private LocalDateTime nextAttemptAt;
    private LocalDateTime lastAttemptAt;
    private LocalDateTime sentAt;
    private LocalDateTime failedAt;

    @Column(length = 500)
    private String failureReason;

    private LocalDateTime claimedAt;

    @Column(length = 100)
    private String claimedBy;

    @Column(nullable = false, length = 100)
    private String templateName;

    @Column(nullable = false, length = 300)
    private String subject;

    @Column(nullable = false)
    private int contentVersion;

    @Override
    @PrePersist
    protected void onCreate() {
        if (getCreatedAt() == null || getUpdatedAt() == null)
            throw new IllegalStateException("Notification delivery audit timestamps are required");
    }

    @Override
    @PreUpdate
    protected void onUpdate() {
        if (getUpdatedAt() == null)
            throw new IllegalStateException("Notification delivery updatedAt is required");
    }
}
