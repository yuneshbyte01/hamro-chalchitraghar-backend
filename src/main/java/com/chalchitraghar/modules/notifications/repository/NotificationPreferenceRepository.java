package com.chalchitraghar.modules.notifications.repository;

import com.chalchitraghar.modules.notifications.entity.NotificationPreference;
import com.chalchitraghar.modules.notifications.enums.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationPreferenceRepository
        extends JpaRepository<NotificationPreference, Long> {
    Optional<NotificationPreference> findByUserIdAndNotificationTypeAndChannel(
            Long userId, NotificationType type, NotificationChannel channel);

    List<NotificationPreference> findByUserIdAndChannel(Long userId, NotificationChannel channel);
}
