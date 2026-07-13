package com.chalchitraghar.modules.notifications.dto.response;

import com.chalchitraghar.modules.notifications.enums.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.List;

public record AdminNotificationDetailResponse(
        Long id,
        Long userId,
        String customerName,
        String maskedEmail,
        NotificationType type,
        NotificationChannel channel,
        String eventKey,
        String title,
        String message,
        JsonNode payload,
        boolean read,
        LocalDateTime readAt,
        LocalDateTime occurredAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<AdminNotificationDeliveryDetailResponse> deliveries) {}
