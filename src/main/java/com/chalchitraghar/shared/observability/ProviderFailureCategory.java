package com.chalchitraghar.shared.observability;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/** Bounded provider failure values safe for structured logs and metrics. */
public enum ProviderFailureCategory {
    TIMEOUT,
    CONNECTION_FAILURE,
    AUTHENTICATION_FAILURE,
    REJECTED,
    UNAVAILABLE,
    INVALID_RESPONSE,
    UNKNOWN;

    public String tag() {
        return name().toLowerCase();
    }

    public static ProviderFailureCategory classify(Throwable failure) {
        Throwable cause = failure;
        while (cause != null) {
            if (cause instanceof SocketTimeoutException) return TIMEOUT;
            if (cause instanceof ConnectException) return CONNECTION_FAILURE;
            cause = cause.getCause();
        }
        if (failure instanceof HttpClientErrorException.Unauthorized
                || failure instanceof HttpClientErrorException.Forbidden) {
            return AUTHENTICATION_FAILURE;
        }
        if (failure instanceof HttpClientErrorException) return REJECTED;
        if (failure instanceof HttpServerErrorException) return UNAVAILABLE;
        if (failure instanceof ResourceAccessException) return CONNECTION_FAILURE;
        return UNKNOWN;
    }
}
