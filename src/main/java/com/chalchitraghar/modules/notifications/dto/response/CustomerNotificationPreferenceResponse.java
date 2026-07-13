package com.chalchitraghar.modules.notifications.dto.response;

import com.chalchitraghar.modules.notifications.enums.*;

public record CustomerNotificationPreferenceResponse(
        NotificationType type,
        NotificationChannel channel,
        boolean enabled,
        boolean configurable,
        String source) {}
