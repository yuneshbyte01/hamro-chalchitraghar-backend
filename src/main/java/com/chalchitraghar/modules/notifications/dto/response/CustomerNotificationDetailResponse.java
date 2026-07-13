package com.chalchitraghar.modules.notifications.dto.response;

import com.chalchitraghar.modules.notifications.enums.NotificationChannel;
import com.chalchitraghar.modules.notifications.enums.NotificationType;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;

public record CustomerNotificationDetailResponse(
        Long id,
        NotificationType type,
        NotificationChannel channel,
        String title,
        String message,
        JsonNode payload,
        boolean read,
        LocalDateTime readAt,
        LocalDateTime occurredAt,
        LocalDateTime createdAt) {}
