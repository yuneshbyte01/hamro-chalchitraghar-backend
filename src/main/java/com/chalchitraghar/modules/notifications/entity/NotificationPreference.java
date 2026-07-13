package com.chalchitraghar.modules.notifications.entity;

import com.chalchitraghar.modules.notifications.enums.*;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.shared.GenericEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
        name = "notification_preferences",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_notification_preferences_user_type_channel",
                        columnNames = {"user_id", "notification_type", "channel"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreference extends GenericEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationChannel channel;

    @Column(nullable = false)
    private boolean enabled;

    private LocalDateTime disabledAt;
}
