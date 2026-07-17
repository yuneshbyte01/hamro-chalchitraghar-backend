package com.chalchitraghar.modules.reporting.analytics.dto;

public record DataQualityAnalyticsResponse(
        long confirmedBookingsWithoutRecognizedPayment,
        long successfulPaymentsOnNonConfirmedBookings,
        long multipleSuccessfulPaymentBookings,
        long failedScheduledReportDeliveries,
        long totalWarnings) {}
