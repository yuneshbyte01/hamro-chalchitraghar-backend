package com.chalchitraghar.modules.notifications.service.impl;

import com.chalchitraghar.modules.notifications.dto.response.CustomerNotificationPreferenceResponse;
import com.chalchitraghar.modules.notifications.entity.NotificationPreference;
import com.chalchitraghar.modules.notifications.enums.*;
import com.chalchitraghar.modules.notifications.repository.NotificationPreferenceRepository;
import com.chalchitraghar.modules.notifications.service.NotificationPreferenceService;
import com.chalchitraghar.modules.users.entity.User;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceServiceImpl implements NotificationPreferenceService {
    private static final Set<NotificationType> CONFIGURABLE =
            EnumSet.of(
                    NotificationType.WELCOME, NotificationType.BOOKING_CREATED,
                    NotificationType.BOOKING_CONFIRMED, NotificationType.BOOKING_CANCELLED,
                    NotificationType.BOOKING_EXPIRED, NotificationType.PAYMENT_SUCCEEDED,
                    NotificationType.PAYMENT_FAILED, NotificationType.SHOW_UPDATED,
                    NotificationType.SHOW_CANCELLED, NotificationType.SHOW_REMINDER);
    private final NotificationPreferenceRepository preferences;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<CustomerNotificationPreferenceResponse> list(User user) {
        Map<NotificationType, NotificationPreference> overrides =
                new EnumMap<>(NotificationType.class);
        preferences
                .findByUserIdAndChannel(user.getId(), NotificationChannel.EMAIL)
                .forEach(p -> overrides.put(p.getNotificationType(), p));
        return CONFIGURABLE.stream()
                .sorted(Comparator.comparing(Enum::name))
                .map(type -> response(type, overrides.get(type)))
                .toList();
    }

    @Override
    @Transactional
    public CustomerNotificationPreferenceResponse update(
            User user, NotificationType type, boolean enabled) {
        requireConfigurable(type);
        LocalDateTime now = LocalDateTime.now(clock);
        NotificationPreference preference =
                preferences
                        .findByUserIdAndNotificationTypeAndChannel(
                                user.getId(), type, NotificationChannel.EMAIL)
                        .orElseGet(
                                () -> {
                                    NotificationPreference created =
                                            NotificationPreference.builder()
                                                    .user(user)
                                                    .notificationType(type)
                                                    .channel(NotificationChannel.EMAIL)
                                                    .build();
                                    created.setCreatedAt(now);
                                    return created;
                                });
        preference.setEnabled(enabled);
        preference.setDisabledAt(enabled ? null : now);
        preference.setUpdatedAt(now);
        return response(type, preferences.save(preference));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean emailEnabled(Long userId, NotificationType type) {
        if (!CONFIGURABLE.contains(type)) return true;
        return preferences
                .findByUserIdAndNotificationTypeAndChannel(userId, type, NotificationChannel.EMAIL)
                .map(NotificationPreference::isEnabled)
                .orElse(true);
    }

    private void requireConfigurable(NotificationType type) {
        if (type == null || !CONFIGURABLE.contains(type))
            throw new IllegalArgumentException("Notification type is not configurable for email");
    }

    private CustomerNotificationPreferenceResponse response(
            NotificationType type, NotificationPreference value) {
        return new CustomerNotificationPreferenceResponse(
                type,
                NotificationChannel.EMAIL,
                value == null || value.isEnabled(),
                true,
                value == null ? "DEFAULT" : "USER_OVERRIDE");
    }
}
