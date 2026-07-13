package com.chalchitraghar.modules.audit.enums;

/** Hard deletion is unsupported because it would erase event-ID deduplication history. */
public enum AuditRetentionMode {
    ANONYMIZE
}
