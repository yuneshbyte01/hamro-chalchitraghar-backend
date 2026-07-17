package com.chalchitraghar.applications.admin;

import com.chalchitraghar.modules.reporting.analytics.*;
import com.chalchitraghar.modules.reporting.analytics.dto.*;
import com.chalchitraghar.modules.reporting.dto.request.ReportingDateRange;
import com.chalchitraghar.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reports/analytics")
@RequiredArgsConstructor
@Tag(name = "Admin Reports", description = "ADMIN-only explainable aggregate business analytics")
@SecurityRequirement(name = "bearerAuth")
public class AdminReportingAnalyticsController {
    private final ReportingAnalyticsService analytics;
    private final Clock clock;

    @GetMapping("/overview")
    @Operation(
            summary = "Get compact advanced analytics overview",
            description =
                    "Aggregate-only metrics. Currency-dependent values are null when no single currency can be selected safely.")
    public ResponseEntity<ApiResponse<AnalyticsOverviewResponse>> overview(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String currency) {
        return ok(
                "Analytics overview fetched",
                analytics.overview(range(startDate, endDate), currency));
    }

    @GetMapping("/booking-patterns")
    @Operation(
            summary = "Analyze booking creation patterns",
            description =
                    "Measures stored booking creation attempts, not website traffic or abandoned carts. HOUR_OF_DAY returns 24 buckets and DAY_OF_WEEK returns Monday through Sunday.")
    public ResponseEntity<ApiResponse<List<BookingPatternBucketResponse>>> patterns(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "HOUR_OF_DAY") BookingPatternGrouping groupBy) {
        return ok(
                "Booking patterns fetched",
                analytics.bookingPatterns(range(startDate, endDate), groupBy));
    }

    @GetMapping("/show-times")
    @Operation(
            summary = "Analyze show-start-time performance",
            description =
                    "MORNING 05:00–11:59, AFTERNOON 12:00–16:59, EVENING 17:00–20:59, NIGHT 21:00–04:59. Cancelled shows are excluded.")
    public ResponseEntity<ApiResponse<List<ShowTimeAnalyticsResponse>>> showTimes(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String currency,
            @RequestParam(defaultValue = "TIME_SLOT") ShowTimeGrouping groupBy) {
        return ok(
                "Show-time analytics fetched",
                analytics.showTimes(range(startDate, endDate), currency, groupBy));
    }

    @GetMapping("/seats")
    @Operation(
            summary = "Analyze historical seat utilization",
            description =
                    "Sold seats come from confirmed booking-seat snapshots; sold value uses immutable booking_seats.unit_price.")
    public ResponseEntity<ApiResponse<List<SeatUtilizationRowResponse>>> seats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) Long hallId,
            @RequestParam(required = false) String seatType) {
        return ok(
                "Seat analytics fetched",
                analytics.seats(range(startDate, endDate), movieId, hallId, seatType));
    }

    @GetMapping("/customers")
    @Operation(
            summary = "Get aggregate customer behavior",
            description =
                    "Repeat means at least two confirmed bookings inside the selected period. No customer identifiers are returned.")
    public ResponseEntity<ApiResponse<CustomerAnalyticsResponse>> customers(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ok("Customer analytics fetched", analytics.customers(range(startDate, endDate)));
    }

    @GetMapping("/conversion")
    @Operation(
            summary = "Get booking/payment attempt conversion",
            description =
                    "Booking conversion is confirmed bookings divided by bookings created, not website conversion. Payment rates use terminal attempts only.")
    public ResponseEntity<ApiResponse<ConversionAnalyticsResponse>> conversion(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ok("Conversion analytics fetched", analytics.conversion(range(startDate, endDate)));
    }

    @GetMapping("/refunds")
    @Operation(summary = "Get currency-safe refund analytics")
    public ResponseEntity<ApiResponse<RefundAnalyticsResponse>> refunds(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String currency) {
        return ok(
                "Refund analytics fetched", analytics.refunds(range(startDate, endDate), currency));
    }

    @GetMapping("/revenue-concentration")
    @Operation(summary = "Rank revenue concentration in one required currency")
    public ResponseEntity<ApiResponse<RevenueConcentrationResponse>> concentration(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam String currency,
            @RequestParam(defaultValue = "MOVIE") ConcentrationDimension dimension,
            @RequestParam(defaultValue = "5") int top) {
        return ok(
                "Revenue concentration fetched",
                analytics.concentration(range(startDate, endDate), currency, dimension, top));
    }

    @GetMapping("/performance-extremes")
    @Operation(
            summary = "Get best and lowest eligible performers",
            description =
                    "Small samples can distort rankings. Revenue extremes are omitted unless a currency is supplied.")
    public ResponseEntity<ApiResponse<PerformanceExtremesResponse>> extremes(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String currency,
            @RequestParam(defaultValue = "1") int minimumShows) {
        return ok(
                "Performance extremes fetched",
                analytics.extremes(range(startDate, endDate), currency, minimumShows));
    }

    @GetMapping("/data-quality")
    @Operation(summary = "Get compact reporting consistency warning counts")
    public ResponseEntity<ApiResponse<DataQualityAnalyticsResponse>> quality() {
        return ok("Data-quality analytics fetched", analytics.dataQuality());
    }

    private ReportingDateRange range(LocalDate start, LocalDate end) {
        return ReportingDateRange.of(start, end, clock);
    }

    private <T> ResponseEntity<ApiResponse<T>> ok(String message, T data) {
        return ResponseEntity.ok(ApiResponse.success(message, data));
    }
}
