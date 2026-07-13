package com.chalchitraghar.modules.notifications.service;

import com.chalchitraghar.modules.notifications.dto.request.NotificationSearchCriteria;
import com.chalchitraghar.modules.notifications.dto.response.CustomerNotificationDetailResponse;
import com.chalchitraghar.modules.notifications.dto.response.CustomerNotificationSummaryResponse;
import com.chalchitraghar.modules.notifications.dto.response.ReadAllNotificationsResponse;
import com.chalchitraghar.modules.notifications.dto.response.UnreadNotificationCountResponse;
import com.chalchitraghar.modules.notifications.entity.Notification;
import com.chalchitraghar.modules.notifications.enums.NotificationType;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.shared.response.PageResponse;
import java.time.LocalDateTime;

public interface NotificationService {
    Notification createInAppNotification(
            Long userId,
            NotificationType type,
            String eventKey,
            String title,
            String message,
            String payload,
            LocalDateTime occurredAt);

    PageResponse<CustomerNotificationSummaryResponse> getCustomerNotifications(
            User user, NotificationSearchCriteria criteria, int page, int size, String sortDir);

    CustomerNotificationDetailResponse getCustomerNotification(Long notificationId, User user);

    CustomerNotificationDetailResponse markRead(Long notificationId, User user);

    ReadAllNotificationsResponse markAllRead(User user);

    UnreadNotificationCountResponse unreadCount(User user);
}
