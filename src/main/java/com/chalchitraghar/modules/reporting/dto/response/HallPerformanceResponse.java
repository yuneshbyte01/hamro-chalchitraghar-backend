package com.chalchitraghar.modules.reporting.dto.response;

import com.chalchitraghar.modules.halls.enums.Status;
import java.math.BigDecimal;
import java.util.List;

public record HallPerformanceResponse(
        long hallId,
        String hallName,
        Status hallStatus,
        long configuredCapacity,
        long showCount,
        long confirmedBookingCount,
        long ticketsSold,
        long generatedSeatCount,
        BigDecimal occupancyPercentage,
        List<CurrencyPerformanceAmountResponse> revenueByCurrency,
        BigDecimal averageAttendancePerShow) {}
