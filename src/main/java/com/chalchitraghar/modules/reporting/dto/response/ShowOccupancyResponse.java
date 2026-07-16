package com.chalchitraghar.modules.reporting.dto.response;

import com.chalchitraghar.modules.shows.enums.ShowStatus;
import java.math.BigDecimal;
import java.time.*;

public record ShowOccupancyResponse(
        long showId,
        long movieId,
        String movieTitle,
        long hallId,
        String hallName,
        LocalDate showDate,
        LocalTime showTime,
        ShowStatus showStatus,
        long generatedSeatCount,
        long soldSeatCount,
        long availableSeatCount,
        BigDecimal occupancyPercentage,
        boolean measurable) {}
