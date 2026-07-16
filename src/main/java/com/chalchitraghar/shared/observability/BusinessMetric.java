package com.chalchitraghar.shared.observability;

public enum BusinessMetric {
    BOOKING("chalchitraghar.booking.operations", "chalchitraghar.booking.duration"),
    SEAT_LOCK("chalchitraghar.seat.lock.operations", "chalchitraghar.seat.lock.duration"),
    PAYMENT("chalchitraghar.payment.operations", "chalchitraghar.payment.operation.duration"),
    TICKET("chalchitraghar.ticket.operations", "chalchitraghar.ticket.operation.duration"),
    TICKET_VALIDATION(
            "chalchitraghar.ticket.validation", "chalchitraghar.ticket.validation.duration"),
    NOTIFICATION("chalchitraghar.notification.operations", "chalchitraghar.notification.duration"),
    AUDIT("chalchitraghar.audit.operations", "chalchitraghar.audit.duration"),
    REFUND("chalchitraghar.refund.operations", "chalchitraghar.refund.processing.duration"),
    EMAIL("chalchitraghar.email.delivery", "chalchitraghar.email.delivery.duration");

    private final String counter;
    private final String timer;

    BusinessMetric(String counter, String timer) {
        this.counter = counter;
        this.timer = timer;
    }

    String counter() {
        return counter;
    }

    String timer() {
        return timer;
    }
}
