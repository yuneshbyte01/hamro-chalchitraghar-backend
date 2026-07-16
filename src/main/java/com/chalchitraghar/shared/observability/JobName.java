package com.chalchitraghar.shared.observability;

public enum JobName {
    BOOKING_EXPIRY,
    SEAT_LOCK_EXPIRY,
    PAYMENT_RECONCILIATION,
    PAYMENT_EXPIRY,
    SHOW_RECONCILIATION,
    TICKET_EXPIRY,
    TICKET_DELIVERY_RETRY,
    NOTIFICATION_DELIVERY_RETRY,
    SHOW_REMINDER,
    NOTIFICATION_RETENTION,
    AUDIT_RETENTION,
    REFUND_RETRY,
    REFUND_RETENTION;

    String tag() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
