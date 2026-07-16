package com.chalchitraghar.modules.reporting.service;

import com.chalchitraghar.modules.reporting.dto.request.ReportGrouping;
import java.time.*;
import java.time.temporal.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class ReportPeriodBuckets {
    public List<Period> periods(LocalDate start, LocalDate end, ReportGrouping grouping) {
        List<Period> periods = new ArrayList<>();
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            LocalDate naturalStart = bucketStart(cursor, grouping);
            LocalDate naturalEnd = bucketEnd(naturalStart, grouping);
            LocalDate periodEnd = naturalEnd.isAfter(end) ? end : naturalEnd;
            periods.add(new Period(cursor, periodEnd, naturalStart));
            cursor = periodEnd.plusDays(1);
        }
        return periods;
    }

    public LocalDate key(LocalDate day, ReportGrouping grouping) {
        return bucketStart(day, grouping);
    }

    private LocalDate bucketStart(LocalDate date, ReportGrouping grouping) {
        return switch (grouping) {
            case DAY -> date;
            case WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> date.withDayOfMonth(1);
        };
    }

    private LocalDate bucketEnd(LocalDate start, ReportGrouping grouping) {
        return switch (grouping) {
            case DAY -> start;
            case WEEK -> start.plusDays(6);
            case MONTH -> start.with(TemporalAdjusters.lastDayOfMonth());
        };
    }

    public record Period(LocalDate start, LocalDate end, LocalDate key) {}
}
