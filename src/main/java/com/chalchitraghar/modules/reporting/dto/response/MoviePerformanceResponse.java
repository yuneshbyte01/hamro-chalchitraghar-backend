package com.chalchitraghar.modules.reporting.dto.response;

import com.chalchitraghar.modules.movies.enums.MovieStatus;
import java.math.BigDecimal;
import java.util.List;

public record MoviePerformanceResponse(
        long movieId,
        String movieTitle,
        MovieStatus movieStatus,
        long showCount,
        long confirmedBookingCount,
        long ticketsSold,
        long generatedSeatCount,
        BigDecimal occupancyPercentage,
        List<CurrencyPerformanceAmountResponse> revenueByCurrency,
        BigDecimal averageAttendancePerShow) {}
