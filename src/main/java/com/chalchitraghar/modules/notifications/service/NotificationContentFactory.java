package com.chalchitraghar.modules.notifications.service;

import com.chalchitraghar.modules.notifications.enums.NotificationType;
import com.chalchitraghar.modules.notifications.event.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationContentFactory {
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final ObjectMapper json;

    public NotificationContent build(Object event) {
        if (event instanceof UserRegisteredEvent e)
            return content(
                    NotificationType.WELCOME,
                    "USER_REGISTERED:" + e.userId(),
                    "Welcome",
                    "Welcome to Hamro Chalchitraghar.",
                    payload());
        if (event instanceof BookingCreatedEvent e)
            return content(
                    NotificationType.BOOKING_CREATED,
                    "BOOKING_CREATED:" + e.bookingId(),
                    "Booking initiated",
                    "Your booking "
                            + e.bookingReference()
                            + " for "
                            + e.movieName()
                            + " has been initiated. Complete payment to confirm it.",
                    payload("bookingReference", e.bookingReference()));
        if (event instanceof BookingConfirmedEvent e)
            return content(
                    NotificationType.BOOKING_CONFIRMED,
                    "BOOKING_CONFIRMED:" + e.bookingId(),
                    "Booking confirmed",
                    "Your booking "
                            + e.bookingReference()
                            + " for "
                            + e.movieName()
                            + " has been confirmed.",
                    payload("bookingReference", e.bookingReference()));
        if (event instanceof BookingCancelledEvent e)
            return content(
                    NotificationType.BOOKING_CANCELLED,
                    "BOOKING_CANCELLED:" + e.bookingId(),
                    "Booking cancelled",
                    "Your booking "
                            + e.bookingReference()
                            + " for "
                            + e.movieName()
                            + " has been cancelled.",
                    payload("bookingReference", e.bookingReference()));
        if (event instanceof BookingExpiredEvent e)
            return content(
                    NotificationType.BOOKING_EXPIRED,
                    "BOOKING_EXPIRED:" + e.bookingId(),
                    "Booking expired",
                    "Your booking "
                            + e.bookingReference()
                            + " for "
                            + e.movieName()
                            + " expired before confirmation.",
                    payload("bookingReference", e.bookingReference()));
        if (event instanceof PaymentSucceededEvent e)
            return content(
                    NotificationType.PAYMENT_SUCCEEDED,
                    "PAYMENT_SUCCEEDED:" + e.paymentId(),
                    "Payment successful",
                    "Payment for booking " + e.bookingReference() + " was completed successfully.",
                    payload(
                            "bookingReference",
                            e.bookingReference(),
                            "paymentReference",
                            e.paymentReference()));
        if (event instanceof PaymentFailedEvent e)
            return content(
                    NotificationType.PAYMENT_FAILED,
                    "PAYMENT_FAILED:" + e.paymentId(),
                    "Payment unsuccessful",
                    "Payment for booking "
                            + e.bookingReference()
                            + " was unsuccessful. You may try again.",
                    payload(
                            "bookingReference",
                            e.bookingReference(),
                            "paymentReference",
                            e.paymentReference()));
        if (event instanceof ShowUpdatedEvent e)
            return content(
                    NotificationType.SHOW_UPDATED,
                    "SHOW_UPDATED:" + e.showId() + ":" + e.changeVersion() + ":" + e.userId(),
                    "Show schedule updated",
                    "The show for "
                            + e.movieName()
                            + " has moved from "
                            + DATE_TIME.format(e.oldSchedule())
                            + " to "
                            + DATE_TIME.format(e.newSchedule())
                            + " at "
                            + e.hallName()
                            + ".",
                    payload("showDateTime", e.newSchedule().toString()));
        if (event instanceof ShowCancelledEvent e)
            return content(
                    NotificationType.SHOW_CANCELLED,
                    "SHOW_CANCELLED:" + e.showId() + ":" + e.userId(),
                    "Show cancelled",
                    "The show for "
                            + e.movieName()
                            + " scheduled on "
                            + DATE_TIME.format(e.showDateTime())
                            + " has been cancelled.",
                    payload("showDateTime", e.showDateTime().toString()));
        if (event instanceof ShowReminderDueEvent e)
            return content(
                    NotificationType.SHOW_REMINDER,
                    "SHOW_REMINDER:" + e.bookingId() + ":" + e.reminderWindow(),
                    "Your show starts soon",
                    "Your booking "
                            + e.bookingReference()
                            + " for "
                            + e.movieName()
                            + " starts at "
                            + DATE_TIME.format(e.showDateTime())
                            + " in "
                            + e.hallName()
                            + ".",
                    payload(
                            "bookingReference", e.bookingReference(),
                            "showDateTime", e.showDateTime().toString(),
                            "hallName", e.hallName()));
        if (event instanceof TicketIssuedNotificationEvent e)
            return content(
                    NotificationType.TICKET_ISSUED,
                    "TICKET_ISSUED:" + e.bookingId(),
                    "Tickets ready",
                    e.ticketCount()
                            + (e.ticketCount() == 1 ? " ticket is" : " tickets are")
                            + " ready for booking "
                            + e.bookingReference()
                            + " for "
                            + e.movieName()
                            + ".",
                    payload(
                            "bookingReference",
                            e.bookingReference(),
                            "ticketCount",
                            e.ticketCount()));
        throw new IllegalArgumentException(
                "Unsupported notification event: " + event.getClass().getName());
    }

    private NotificationContent content(
            NotificationType type, String key, String title, String message, String payload) {
        return new NotificationContent(type, key, title, message, payload);
    }

    private String payload(Object... entries) {
        var values = new LinkedHashMap<String, Object>();
        for (int i = 0; i < entries.length; i += 2) values.put((String) entries[i], entries[i + 1]);
        try {
            return values.isEmpty() ? null : json.writeValueAsString(values);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to build safe notification payload", exception);
        }
    }
}
