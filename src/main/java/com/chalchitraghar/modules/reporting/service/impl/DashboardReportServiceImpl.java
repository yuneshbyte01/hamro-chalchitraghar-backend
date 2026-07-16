package com.chalchitraghar.modules.reporting.service.impl;

import com.chalchitraghar.modules.movies.enums.MovieStatus;
import com.chalchitraghar.modules.reporting.dto.request.ReportingDateRange;
import com.chalchitraghar.modules.reporting.dto.response.*;
import com.chalchitraghar.modules.reporting.repository.ReportingQueryRepository;
import com.chalchitraghar.modules.reporting.service.*;
import com.chalchitraghar.modules.shows.enums.ShowStatus;
import com.chalchitraghar.modules.users.enums.Role;
import java.time.*;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardReportServiceImpl implements DashboardReportService {
    private final BookingReportService bookings;
    private final RevenueReportService revenue;
    private final ReportingQueryRepository repository;
    private final Clock clock;

    @Override
    public AdminDashboardSummaryResponse getSummary(ReportingDateRange range, String currency) {
        return new AdminDashboardSummaryResponse(
                ReportingPeriodResponse.from(range),
                bookings.getKpis(range),
                revenue.getKpis(range, currency),
                repository.registeredUsers(
                        Role.CUSTOMER, range.startInclusive(), range.endExclusive()),
                repository.moviesByStatus(Set.of(MovieStatus.NOW_SHOWING, MovieStatus.UPCOMING)),
                repository.showsByStatus(ShowStatus.RUNNING),
                repository.showsByStatus(ShowStatus.SCHEDULED),
                LocalDateTime.now(clock));
    }
}
