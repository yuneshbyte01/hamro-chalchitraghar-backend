package com.chalchitraghar.modules.reporting.analytics;

import com.chalchitraghar.modules.reporting.analytics.dto.*;
import com.chalchitraghar.modules.reporting.dto.request.ReportingDateRange;
import java.util.List;

public interface ReportingAnalyticsService {
    AnalyticsOverviewResponse overview(ReportingDateRange range, String currency);

    List<BookingPatternBucketResponse> bookingPatterns(
            ReportingDateRange range, BookingPatternGrouping grouping);

    List<ShowTimeAnalyticsResponse> showTimes(
            ReportingDateRange range, String currency, ShowTimeGrouping grouping);

    List<SeatUtilizationRowResponse> seats(
            ReportingDateRange range, Long movieId, Long hallId, String seatType);

    CustomerAnalyticsResponse customers(ReportingDateRange range);

    ConversionAnalyticsResponse conversion(ReportingDateRange range);

    RefundAnalyticsResponse refunds(ReportingDateRange range, String currency);

    RevenueConcentrationResponse concentration(
            ReportingDateRange range, String currency, ConcentrationDimension dimension, int top);

    PerformanceExtremesResponse extremes(
            ReportingDateRange range, String currency, int minimumShows);

    DataQualityAnalyticsResponse dataQuality();
}
