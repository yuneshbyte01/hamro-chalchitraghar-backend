package com.chalchitraghar.modules.notifications.service;

import com.chalchitraghar.modules.notifications.enums.NotificationType;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NotificationEmailPolicy {
    private static final Map<NotificationType, String> TEMPLATES =
            Map.of(
                    NotificationType.WELCOME, "welcome",
                    NotificationType.BOOKING_CONFIRMED, "booking-confirmed",
                    NotificationType.BOOKING_CANCELLED, "booking-cancelled",
                    NotificationType.BOOKING_EXPIRED, "booking-expired",
                    NotificationType.PAYMENT_SUCCEEDED, "payment-succeeded",
                    NotificationType.PAYMENT_FAILED, "payment-failed",
                    NotificationType.SHOW_UPDATED, "show-updated",
                    NotificationType.SHOW_CANCELLED, "show-cancelled");

    public boolean eligible(NotificationType type) {
        return TEMPLATES.containsKey(type);
    }

    public String template(NotificationType type) {
        return TEMPLATES.get(type);
    }
}
