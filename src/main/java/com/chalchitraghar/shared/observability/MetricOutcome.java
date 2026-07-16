package com.chalchitraghar.shared.observability;

import com.chalchitraghar.shared.exception.AuthenticationException;
import com.chalchitraghar.shared.exception.InvalidBookingStateException;
import com.chalchitraghar.shared.exception.PaymentConflictException;
import com.chalchitraghar.shared.exception.SeatAlreadyBookedException;
import com.chalchitraghar.shared.exception.SeatLockedException;

public enum MetricOutcome {
    SUCCESS,
    FAILURE,
    CONFLICT,
    DENIED,
    SKIPPED,
    PARTIAL;

    static MetricOutcome from(Throwable failure) {
        if (failure instanceof SeatLockedException
                || failure instanceof SeatAlreadyBookedException
                || failure instanceof InvalidBookingStateException
                || failure instanceof PaymentConflictException) return CONFLICT;
        if (failure instanceof AuthenticationException
                || failure instanceof org.springframework.security.access.AccessDeniedException)
            return DENIED;
        return FAILURE;
    }

    public String tag() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
