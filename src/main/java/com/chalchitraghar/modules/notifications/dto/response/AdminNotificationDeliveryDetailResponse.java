package com.chalchitraghar.modules.notifications.dto.response;

import com.chalchitraghar.modules.notifications.enums.*;
import java.time.LocalDateTime;

public record AdminNotificationDeliveryDetailResponse(
        Long deliveryId,
        NotificationChannel channel,
        String maskedRecipient,
        NotificationDeliveryStatus status,
        int attemptCount,
        int maxAttempts,
        LocalDateTime nextAttemptAt,
        LocalDateTime lastAttemptAt,
        LocalDateTime sentAt,
        LocalDateTime failedAt,
        String sanitizedFailureReason,
        LocalDateTime claimedAt,
        String claimedBy,
        String templateName,
        String subject,
        int contentVersion,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
