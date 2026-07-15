package com.chalchitraghar.modules.payments.gateway;

public enum RefundGatewayOutcome {
    SUCCEEDED,
    FAILED_RETRYABLE,
    FAILED_TERMINAL,
    UNKNOWN,
    MANUAL_REVIEW
}
