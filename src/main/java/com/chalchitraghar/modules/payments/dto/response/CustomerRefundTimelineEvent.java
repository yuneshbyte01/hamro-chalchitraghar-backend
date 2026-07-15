package com.chalchitraghar.modules.payments.dto.response;

import java.time.LocalDateTime;

public record CustomerRefundTimelineEvent(String event, LocalDateTime occurredAt, String message) {}
