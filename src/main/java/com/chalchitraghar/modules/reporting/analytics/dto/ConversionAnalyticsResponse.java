package com.chalchitraghar.modules.reporting.analytics.dto;

import java.math.BigDecimal;
import java.util.Map;

public record ConversionAnalyticsResponse(
        long totalBookingAttempts,
        long confirmedBookings,
        long cancelledBookings,
        long expiredBookings,
        long initiatedOrPendingBookings,
        BigDecimal bookingAttemptConversionRate,
        BigDecimal cancellationRate,
        BigDecimal expirationRate,
        long terminalPaymentAttempts,
        long successfulPaymentAttempts,
        long failedPaymentAttempts,
        long expiredPaymentAttempts,
        BigDecimal paymentAttemptSuccessRate,
        BigDecimal paymentAttemptFailureRate,
        BigDecimal paymentAttemptExpiryRate,
        Map<String, Long> bookingStatusBreakdown) {}
