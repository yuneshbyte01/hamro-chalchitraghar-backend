package com.chalchitraghar.modules.notifications.service;

import com.chalchitraghar.modules.notifications.dto.response.*;
import com.chalchitraghar.modules.notifications.enums.*;
import com.chalchitraghar.shared.response.PageResponse;
import java.time.LocalDateTime;

public interface AdminNotificationOperationsService {
    PageResponse<AdminNotificationSummaryResponse> notifications(
            int page,
            int size,
            Long userId,
            String email,
            NotificationType type,
            NotificationChannel channel,
            Boolean read,
            LocalDateTime occurredFrom,
            LocalDateTime occurredTo,
            NotificationDeliveryStatus deliveryStatus,
            String eventKey);

    AdminNotificationDetailResponse notification(Long id);

    PageResponse<AdminNotificationDeliverySummaryResponse> deliveries(
            int page,
            int size,
            NotificationDeliveryStatus status,
            NotificationType type,
            Integer minAttempts,
            Integer maxAttempts);

    AdminNotificationDeliveryDetailResponse delivery(Long id);

    ManualDeliveryRetryResponse retry(Long id);
}
