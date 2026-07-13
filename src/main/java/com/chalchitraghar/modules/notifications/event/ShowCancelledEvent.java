package com.chalchitraghar.modules.notifications.event;

import java.time.LocalDateTime;

public record ShowCancelledEvent(
        Long userId,
        Long showId,
        String movieName,
        LocalDateTime showDateTime,
        LocalDateTime occurredAt) {}
