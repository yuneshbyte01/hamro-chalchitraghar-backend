package com.chalchitraghar.modules.notifications.event;

import com.chalchitraghar.modules.audit.enums.AuditAction;
import java.time.LocalDateTime;

public record RefundOperationalAuditEvent(
        Long refundId,
        String refundReference,
        Long actorUserId,
        AuditAction action,
        String beforeStatus,
        String afterStatus,
        LocalDateTime occurredAt) {}
