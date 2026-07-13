package com.chalchitraghar.modules.notifications.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateNotificationPreferenceRequest(@NotNull Boolean enabled) {}
