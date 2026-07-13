package com.chalchitraghar.modules.notifications.dto.response;

import com.chalchitraghar.modules.notifications.enums.NotificationChannel;
import com.chalchitraghar.modules.notifications.enums.NotificationType;
import java.time.LocalDateTime;

public record CustomerNotificationSummaryResponse(
        Long id,
        NotificationType type,
        NotificationChannel channel,
        String title,
        String messagePreview,
        boolean read,
        LocalDateTime readAt,
        LocalDateTime occurredAt) {}
