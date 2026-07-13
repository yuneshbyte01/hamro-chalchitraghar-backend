package com.chalchitraghar.modules.notifications.service.impl;

import com.chalchitraghar.modules.notifications.dto.request.NotificationSearchCriteria;
import com.chalchitraghar.modules.notifications.dto.response.CustomerNotificationDetailResponse;
import com.chalchitraghar.modules.notifications.dto.response.CustomerNotificationSummaryResponse;
import com.chalchitraghar.modules.notifications.dto.response.ReadAllNotificationsResponse;
import com.chalchitraghar.modules.notifications.dto.response.UnreadNotificationCountResponse;
import com.chalchitraghar.modules.notifications.entity.Notification;
import com.chalchitraghar.modules.notifications.enums.NotificationChannel;
import com.chalchitraghar.modules.notifications.enums.NotificationType;
import com.chalchitraghar.modules.notifications.mapper.NotificationMapper;
import com.chalchitraghar.modules.notifications.repository.NotificationRepository;
import com.chalchitraghar.modules.notifications.service.NotificationService;
import com.chalchitraghar.modules.notifications.specification.NotificationSpecification;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.modules.users.repository.UserRepository;
import com.chalchitraghar.shared.exception.ResourceNotFoundException;
import com.chalchitraghar.shared.response.PageResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_EVENT_KEY_LENGTH = 200;
    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_MESSAGE_LENGTH = 2000;
    private static final int MAX_PAYLOAD_LENGTH = 4000;

    private final NotificationRepository notifications;
    private final UserRepository users;
    private final NotificationMapper mapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final PlatformTransactionManager transactionManager;

    @Override
    public Notification createInAppNotification(
            Long userId,
            NotificationType type,
            String eventKey,
            String title,
            String message,
            String payload,
            LocalDateTime occurredAt) {
        if (userId == null) throw new IllegalArgumentException("User is required");
        if (type == null) throw new IllegalArgumentException("Notification type is required");
        String normalizedEventKey = required(eventKey, "Event key", MAX_EVENT_KEY_LENGTH);
        String normalizedTitle = required(title, "Title", MAX_TITLE_LENGTH);
        String normalizedMessage = required(message, "Message", MAX_MESSAGE_LENGTH);
        String normalizedPayload = validatePayload(payload);
        LocalDateTime eventTime = occurredAt == null ? LocalDateTime.now(clock) : occurredAt;

        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        try {
            return transaction.execute(
                    status ->
                            notifications
                                    .findByUserIdAndEventKeyAndChannel(
                                            userId, normalizedEventKey, NotificationChannel.IN_APP)
                                    .orElseGet(
                                            () -> {
                                                User user =
                                                        users.findById(userId)
                                                                .orElseThrow(
                                                                        () ->
                                                                                new ResourceNotFoundException(
                                                                                        "User",
                                                                                        userId));
                                                LocalDateTime auditTime = LocalDateTime.now(clock);
                                                Notification notification =
                                                        Notification.builder()
                                                                .user(user)
                                                                .type(type)
                                                                .channel(NotificationChannel.IN_APP)
                                                                .eventKey(normalizedEventKey)
                                                                .title(normalizedTitle)
                                                                .message(normalizedMessage)
                                                                .payload(normalizedPayload)
                                                                .read(false)
                                                                .readAt(null)
                                                                .occurredAt(eventTime)
                                                                .build();
                                                notification.setCreatedAt(auditTime);
                                                notification.setUpdatedAt(auditTime);
                                                return notifications.saveAndFlush(notification);
                                            }));
        } catch (DataIntegrityViolationException race) {
            return new TransactionTemplate(transactionManager)
                    .execute(
                            status ->
                                    notifications
                                            .findByUserIdAndEventKeyAndChannel(
                                                    userId,
                                                    normalizedEventKey,
                                                    NotificationChannel.IN_APP)
                                            .orElseThrow(() -> race));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CustomerNotificationSummaryResponse> getCustomerNotifications(
            User user, NotificationSearchCriteria criteria, int page, int size, String sortDir) {
        validateUser(user);
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE)
            throw new IllegalArgumentException(
                    "Page must be non-negative and size must be 1 to 100");
        if (!"asc".equalsIgnoreCase(sortDir) && !"desc".equalsIgnoreCase(sortDir))
            throw new IllegalArgumentException("Sort direction must be asc or desc");
        if (criteria.channel() == NotificationChannel.EMAIL)
            throw new IllegalArgumentException("Notification-1 supports only IN_APP notifications");
        if (criteria.occurredFrom() != null
                && criteria.occurredTo() != null
                && criteria.occurredFrom().isAfter(criteria.occurredTo()))
            throw new IllegalArgumentException("occurredFrom must not be after occurredTo");

        Sort.Direction direction = Sort.Direction.fromString(sortDir);
        var result =
                notifications.findAll(
                        NotificationSpecification.customer(user.getId(), criteria),
                        PageRequest.of(
                                page,
                                size,
                                Sort.by(
                                        new Sort.Order(direction, "occurredAt"),
                                        new Sort.Order(direction, "id"))));
        return PageResponse.from(result, result.stream().map(mapper::toCustomerSummary).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerNotificationDetailResponse getCustomerNotification(
            Long notificationId, User user) {
        return mapper.toCustomerDetail(owned(notificationId, user));
    }

    @Override
    @Transactional
    public CustomerNotificationDetailResponse markRead(Long notificationId, User user) {
        Notification notification = owned(notificationId, user);
        if (!notification.isRead()) {
            LocalDateTime now = LocalDateTime.now(clock);
            notification.setRead(true);
            notification.setReadAt(now);
            notification.setUpdatedAt(now);
        }
        return mapper.toCustomerDetail(notifications.save(notification));
    }

    @Override
    @Transactional
    public ReadAllNotificationsResponse markAllRead(User user) {
        validateUser(user);
        int updated =
                notifications.markAllRead(
                        user.getId(), NotificationChannel.IN_APP, LocalDateTime.now(clock));
        return new ReadAllNotificationsResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadNotificationCountResponse unreadCount(User user) {
        validateUser(user);
        return new UnreadNotificationCountResponse(
                notifications.countByUserIdAndChannelAndReadFalse(
                        user.getId(), NotificationChannel.IN_APP));
    }

    private Notification owned(Long notificationId, User user) {
        validateUser(user);
        return notifications
                .findByIdAndUserId(notificationId, user.getId())
                .filter(n -> n.getChannel() == NotificationChannel.IN_APP)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));
    }

    private void validateUser(User user) {
        if (user == null || user.getId() == null)
            throw new IllegalArgumentException("Authenticated user is required");
    }

    private String required(String value, String field, int maxLength) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException(field + " is required");
        String trimmed = value.trim();
        if (trimmed.length() > maxLength)
            throw new IllegalArgumentException(
                    field + " must not exceed " + maxLength + " characters");
        return trimmed;
    }

    private String validatePayload(String payload) {
        if (payload == null || payload.isBlank()) return null;
        if (payload.length() > MAX_PAYLOAD_LENGTH)
            throw new IllegalArgumentException("Payload must not exceed 4000 characters");
        try {
            return objectMapper.writeValueAsString(objectMapper.readTree(payload));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Payload must contain valid JSON");
        }
    }
}
