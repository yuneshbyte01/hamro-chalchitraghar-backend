package com.chalchitraghar.modules.notifications.service;

public interface NotificationEmailTemplateService {
    RenderedNotificationEmail render(
            String templateName, String subject, String customerName, String title, String message);
}
