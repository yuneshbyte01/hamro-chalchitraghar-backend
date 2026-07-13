package com.chalchitraghar.modules.audit.dto.response;

import java.time.LocalDateTime;

public record FailedLoginBucketResponse(
        LocalDateTime bucket,
        long total,
        long uniqueActorEmails,
        long accountLocks,
        long invalidCredentials,
        long lockedAccountDenials,
        long disabledAccountDenials,
        long anonymousAttempts) {}
