package com.chalchitraghar.modules.notifications.dto.response;

import com.chalchitraghar.modules.notifications.enums.*;
import java.time.LocalDateTime;

public record AdminNotificationSummaryResponse(
        Long id,
        Long userId,
        String customerName,
        String maskedEmail,
        NotificationType type,
        NotificationChannel channel,
        String title,
        boolean read,
        LocalDateTime occurredAt,
        LocalDateTime createdAt) {}
