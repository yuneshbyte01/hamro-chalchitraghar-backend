package com.chalchitraghar.modules.notifications.service;

import com.chalchitraghar.modules.notifications.entity.Notification;
import com.chalchitraghar.modules.notifications.entity.NotificationDelivery;

public interface NotificationEmailQueueService {
    NotificationDelivery queue(Notification notification);
}
