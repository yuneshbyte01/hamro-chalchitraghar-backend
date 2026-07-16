package com.chalchitraghar.modules.reporting.dto.response;

import com.chalchitraghar.modules.shows.enums.ShowStatus;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public record ShowPerformanceResponse(
        long showId,
        long movieId,
        String movieTitle,
        long hallId,
        String hallName,
        LocalDate showDate,
        LocalTime showTime,
        ShowStatus showStatus,
        long generatedSeatCount,
        long ticketsSold,
        long confirmedBookingCount,
        BigDecimal occupancyPercentage,
        List<CurrencyPerformanceAmountResponse> revenueByCurrency) {}
