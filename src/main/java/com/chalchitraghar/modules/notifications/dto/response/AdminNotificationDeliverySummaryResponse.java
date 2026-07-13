package com.chalchitraghar.modules.notifications.dto.response;

import com.chalchitraghar.modules.notifications.enums.*;
import java.time.LocalDateTime;

public record AdminNotificationDeliverySummaryResponse(
        Long deliveryId,
        NotificationChannel channel,
        String maskedRecipient,
        NotificationDeliveryStatus status,
        int attemptCount,
        int maxAttempts,
        LocalDateTime nextAttemptAt,
        LocalDateTime lastAttemptAt,
        LocalDateTime sentAt,
        LocalDateTime failedAt) {}
