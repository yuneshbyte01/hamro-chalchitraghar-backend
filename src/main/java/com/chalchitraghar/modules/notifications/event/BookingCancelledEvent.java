package com.chalchitraghar.modules.notifications.event;

import java.time.LocalDateTime;

public record BookingCancelledEvent(
        Long userId,
        Long bookingId,
        String bookingReference,
        String movieName,
        LocalDateTime occurredAt) {}
