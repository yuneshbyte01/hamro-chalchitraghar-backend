package com.chalchitraghar.modules.notifications.event;

import java.time.LocalDateTime;

public record ShowUpdatedEvent(
        Long userId,
        Long showId,
        String changeVersion,
        String movieName,
        LocalDateTime oldSchedule,
        LocalDateTime newSchedule,
        String hallName,
        LocalDateTime occurredAt) {}
