package com.chalchitraghar.modules.notifications.event;

import java.time.LocalDateTime;

public record UserRegisteredEvent(Long userId, LocalDateTime occurredAt) {}
