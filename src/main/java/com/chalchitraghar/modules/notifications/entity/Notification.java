package com.chalchitraghar.modules.notifications.entity;

import com.chalchitraghar.modules.notifications.enums.NotificationChannel;
import com.chalchitraghar.modules.notifications.enums.NotificationType;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.shared.GenericEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "notifications",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_notifications_user_event_channel",
                        columnNames = {"user_id", "event_key", "channel"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification extends GenericEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationChannel channel;

    @Column(name = "event_key", nullable = false, updatable = false, length = 200)
    private String eventKey;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 2000)
    private String message;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    /** Notification audit timestamps are assigned by the service using the application Clock. */
    @Override
    @PrePersist
    protected void onCreate() {
        if (getCreatedAt() == null || getUpdatedAt() == null)
            throw new IllegalStateException("Notification audit timestamps are required");
    }

    /** Notification updates must explicitly carry an application-Clock timestamp. */
    @Override
    @PreUpdate
    protected void onUpdate() {
        if (getUpdatedAt() == null)
            throw new IllegalStateException("Notification updatedAt is required");
    }
}
