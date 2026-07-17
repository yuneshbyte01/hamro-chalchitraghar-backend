package com.chalchitraghar;

import static org.assertj.core.api.Assertions.assertThat;

import com.chalchitraghar.modules.reporting.enums.ReportScheduleFrequency;
import com.chalchitraghar.modules.reporting.schedule.ScheduledReportPeriodCalculator;
import java.time.*;
import org.junit.jupiter.api.Test;

class ScheduledReportPeriodCalculatorTest {
    private final ScheduledReportPeriodCalculator calculator =
            new ScheduledReportPeriodCalculator();

    @Test
    void dailyUsesPreviousCompletedLocalDayInKathmandu() {
        Clock clock =
                Clock.fixed(Instant.parse("2026-07-15T18:30:00Z"), ZoneId.of("Asia/Kathmandu"));
        var period = calculator.completed(ReportScheduleFrequency.DAILY, clock);
        assertThat(period.start()).isEqualTo(LocalDate.of(2026, 7, 15));
        assertThat(period.end()).isEqualTo(period.start());
    }

    @Test
    void weeklyUsesPreviousCompletedIsoWeekInUtc() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-16T12:00:00Z"), ZoneOffset.UTC);
        var period = calculator.completed(ReportScheduleFrequency.WEEKLY, clock);
        assertThat(period.start()).isEqualTo(LocalDate.of(2026, 7, 6));
        assertThat(period.end()).isEqualTo(LocalDate.of(2026, 7, 12));
    }

    @Test
    void monthlyHandlesLeapYearAndYearBoundary() {
        Clock leap = Clock.fixed(Instant.parse("2024-03-10T00:00:00Z"), ZoneOffset.UTC);
        assertThat(calculator.completed(ReportScheduleFrequency.MONTHLY, leap).end())
                .isEqualTo(LocalDate.of(2024, 2, 29));
        Clock january = Clock.fixed(Instant.parse("2026-01-10T00:00:00Z"), ZoneOffset.UTC);
        var period = calculator.completed(ReportScheduleFrequency.MONTHLY, january);
        assertThat(period.start()).isEqualTo(LocalDate.of(2025, 12, 1));
        assertThat(period.end()).isEqualTo(LocalDate.of(2025, 12, 31));
    }
}
