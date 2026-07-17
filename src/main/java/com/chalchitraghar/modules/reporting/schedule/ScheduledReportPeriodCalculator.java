package com.chalchitraghar.modules.reporting.schedule;

import com.chalchitraghar.modules.reporting.enums.ReportScheduleFrequency;
import java.time.*;
import java.time.temporal.*;
import org.springframework.stereotype.Component;

@Component
public class ScheduledReportPeriodCalculator {
    public Period completed(ReportScheduleFrequency frequency, Clock clock) {
        LocalDate today = LocalDate.now(clock);
        return switch (frequency) {
            case DAILY -> new Period(today.minusDays(1), today.minusDays(1));
            case WEEKLY -> {
                LocalDate start =
                        today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                                .minusWeeks(1);
                yield new Period(start, start.plusDays(6));
            }
            case MONTHLY -> {
                LocalDate end = today.withDayOfMonth(1).minusDays(1);
                yield new Period(end.withDayOfMonth(1), end);
            }
        };
    }

    public LocalDateTime nextRun(ReportScheduleFrequency frequency, LocalDateTime after) {
        LocalDate date = after.toLocalDate();
        return switch (frequency) {
            case DAILY -> date.plusDays(1).atStartOfDay();
            case WEEKLY -> date.with(TemporalAdjusters.next(DayOfWeek.MONDAY)).atStartOfDay();
            case MONTHLY -> date.withDayOfMonth(1).plusMonths(1).atStartOfDay();
        };
    }

    public record Period(LocalDate start, LocalDate end) {}
}
