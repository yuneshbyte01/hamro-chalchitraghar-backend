package com.chalchitraghar.modules.notifications.mapper;

import com.chalchitraghar.modules.notifications.dto.response.CustomerNotificationDetailResponse;
import com.chalchitraghar.modules.notifications.dto.response.CustomerNotificationSummaryResponse;
import com.chalchitraghar.modules.notifications.entity.Notification;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationMapper {
    private static final int PREVIEW_LENGTH = 120;

    private final ObjectMapper objectMapper;

    public CustomerNotificationSummaryResponse toCustomerSummary(Notification notification) {
        return new CustomerNotificationSummaryResponse(
                notification.getId(),
                notification.getType(),
                notification.getChannel(),
                notification.getTitle(),
                preview(notification.getMessage()),
                notification.isRead(),
                notification.getReadAt(),
                notification.getOccurredAt());
    }

    public CustomerNotificationDetailResponse toCustomerDetail(Notification notification) {
        return new CustomerNotificationDetailResponse(
                notification.getId(),
                notification.getType(),
                notification.getChannel(),
                notification.getTitle(),
                notification.getMessage(),
                parsePayload(notification.getPayload()),
                notification.isRead(),
                notification.getReadAt(),
                notification.getOccurredAt(),
                notification.getCreatedAt());
    }

    private String preview(String message) {
        if (message == null || message.length() <= PREVIEW_LENGTH) return message;
        int end =
                message.offsetByCodePoints(
                        0, Math.min(PREVIEW_LENGTH, message.codePointCount(0, message.length())));
        return message.substring(0, end) + "…";
    }

    private JsonNode parsePayload(String payload) {
        if (payload == null) return null;
        try {
            return objectMapper.readTree(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored notification payload is invalid", exception);
        }
    }
}
