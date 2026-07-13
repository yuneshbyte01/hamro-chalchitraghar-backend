package com.chalchitraghar.modules.notifications.service;

public interface NotificationMailSender {
    void send(String recipient, RenderedNotificationEmail email);
}
