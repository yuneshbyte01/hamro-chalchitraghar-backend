package com.chalchitraghar.modules.notifications.event;

import java.time.LocalDateTime;

public record ShowReminderDueEvent(
        Long userId,
        Long bookingId,
        String bookingReference,
        Long showId,
        String movieName,
        String hallName,
        LocalDateTime showDateTime,
        String reminderWindow,
        LocalDateTime occurredAt) {}
