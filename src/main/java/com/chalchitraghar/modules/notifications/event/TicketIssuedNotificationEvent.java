package com.chalchitraghar.modules.notifications.event;

import java.time.LocalDateTime;

public record TicketIssuedNotificationEvent(
        Long userId,
        Long bookingId,
        String bookingReference,
        String movieName,
        int ticketCount,
        LocalDateTime occurredAt) {}
