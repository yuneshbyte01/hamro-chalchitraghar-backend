package com.chalchitraghar.modules.notifications.service;

import com.chalchitraghar.modules.notifications.enums.NotificationType;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NotificationEmailPolicy {
    private static final Map<NotificationType, String> TEMPLATES =
            Map.ofEntries(
                    Map.entry(NotificationType.WELCOME, "welcome"),
                    Map.entry(NotificationType.BOOKING_CONFIRMED, "booking-confirmed"),
                    Map.entry(NotificationType.BOOKING_CANCELLED, "booking-cancelled"),
                    Map.entry(NotificationType.BOOKING_EXPIRED, "booking-expired"),
                    Map.entry(NotificationType.PAYMENT_SUCCEEDED, "payment-succeeded"),
                    Map.entry(NotificationType.PAYMENT_FAILED, "payment-failed"),
                    Map.entry(NotificationType.REFUND_REQUESTED, "refund-status"),
                    Map.entry(NotificationType.REFUND_APPROVED, "refund-status"),
                    Map.entry(NotificationType.REFUND_REJECTED, "refund-status"),
                    Map.entry(NotificationType.REFUND_MANUAL_REVIEW, "refund-status"),
                    Map.entry(NotificationType.SHOW_UPDATED, "show-updated"),
                    Map.entry(NotificationType.SHOW_CANCELLED, "show-cancelled"),
                    Map.entry(NotificationType.SHOW_REMINDER, "show-reminder"));

    public boolean eligible(NotificationType type) {
        return TEMPLATES.containsKey(type);
    }

    public String template(NotificationType type) {
        return TEMPLATES.get(type);
    }
}
