package com.chalchitraghar.modules.audit.event;

import com.chalchitraghar.modules.audit.enums.AuditActorType;

public record AuditActor(
        AuditActorType type, Long userId, String emailSnapshot, String roleSnapshot) {}
