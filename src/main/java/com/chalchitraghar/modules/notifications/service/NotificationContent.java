package com.chalchitraghar.modules.notifications.service;

import com.chalchitraghar.modules.notifications.enums.NotificationType;

public record NotificationContent(
        NotificationType type, String eventKey, String title, String message, String payload) {}
