package com.chalchitraghar.modules.notifications.service;

import com.chalchitraghar.modules.notifications.dto.response.CustomerNotificationPreferenceResponse;
import com.chalchitraghar.modules.notifications.enums.NotificationType;
import com.chalchitraghar.modules.users.entity.User;
import java.util.List;

public interface NotificationPreferenceService {
    List<CustomerNotificationPreferenceResponse> list(User user);

    CustomerNotificationPreferenceResponse update(
            User user, NotificationType type, boolean enabled);

    boolean emailEnabled(Long userId, NotificationType type);
}
