package com.chalchitraghar.modules.reporting.analytics.dto;

import java.math.BigDecimal;

public record CustomerAnalyticsResponse(
        long uniqueCustomersWithBooking,
        long uniqueCustomersWithConfirmedBooking,
        long firstTimeConfirmedCustomers,
        long repeatConfirmedCustomers,
        BigDecimal repeatCustomerRate,
        BigDecimal averageConfirmedBookingsPerCustomer,
        BigDecimal averageSeatsPerConfirmedCustomer,
        long customersWithCancellations,
        BigDecimal customerCancellationRate) {}
