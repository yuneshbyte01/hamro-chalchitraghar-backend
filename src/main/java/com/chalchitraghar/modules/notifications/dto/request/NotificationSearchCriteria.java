package com.chalchitraghar.modules.notifications.dto.request;

import com.chalchitraghar.modules.notifications.enums.NotificationChannel;
import com.chalchitraghar.modules.notifications.enums.NotificationType;
import java.time.LocalDateTime;

public record NotificationSearchCriteria(
        NotificationType type,
        NotificationChannel channel,
        Boolean read,
        LocalDateTime occurredFrom,
        LocalDateTime occurredTo) {}
