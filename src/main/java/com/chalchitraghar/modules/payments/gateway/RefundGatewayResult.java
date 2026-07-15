package com.chalchitraghar.modules.payments.gateway;

public record RefundGatewayResult(
        RefundGatewayOutcome outcome,
        String providerRefundReference,
        String providerStatus,
        String failureCode,
        String sanitizedMessage) {
    public static RefundGatewayResult manualReview(String code) {
        return new RefundGatewayResult(
                RefundGatewayOutcome.MANUAL_REVIEW,
                null,
                "UNSUPPORTED",
                code,
                "Manual confirmation is required");
    }
}
