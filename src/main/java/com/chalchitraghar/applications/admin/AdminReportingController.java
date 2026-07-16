package com.chalchitraghar.applications.admin;

import com.chalchitraghar.modules.reporting.dto.request.*;
import com.chalchitraghar.modules.reporting.dto.response.*;
import com.chalchitraghar.modules.reporting.service.*;
import com.chalchitraghar.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@Tag(name = "Admin Reports", description = "ADMIN-only business dashboard and core KPI reports")
@SecurityRequirement(name = "bearerAuth")
public class AdminReportingController {
    private final DashboardReportService dashboard;
    private final BookingReportService bookings;
    private final RevenueReportService revenue;
    private final OccupancyReportService occupancy;
    private final PerformanceReportService performance;
    private final Clock clock;

    @GetMapping("/dashboard")
    @Operation(
            summary = "Get the combined admin dashboard",
            description =
                    "Returns period booking, revenue and customer KPIs plus current active-movie and show-status snapshots. API dates are inclusive and converted to an exclusive next-day boundary in the configured application timezone. Revenue remains separated by currency; successful payment attempts are counted individually.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Report generated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Validation failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Access denied")
    })
    public ResponseEntity<ApiResponse<AdminDashboardSummaryResponse>> dashboard(
            @Parameter(required = true, example = "2026-07-01")
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate startDate,
            @Parameter(required = true, example = "2026-07-31")
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate endDate,
            @Parameter(
                            description =
                                    "Optional three-letter currency; whitespace is trimmed and letters are uppercased",
                            example = "NPR")
                    @RequestParam(required = false)
                    String currency) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Dashboard report fetched successfully",
                        dashboard.getSummary(range(startDate, endDate), currency)));
    }

    @GetMapping("/dashboard/bookings")
    @Operation(
            summary = "Get booking KPIs",
            description =
                    "Returns all booking statuses and percentage rates for a bounded inclusive local-date range. Missing statuses are returned with zero counts.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Report generated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Validation failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Access denied")
    })
    public ResponseEntity<ApiResponse<AdminBookingKpiResponse>> bookings(
            @Parameter(required = true, example = "2026-07-01")
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate startDate,
            @Parameter(required = true, example = "2026-07-31")
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate endDate) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Booking KPI report fetched successfully",
                        bookings.getKpis(range(startDate, endDate))));
    }

    @GetMapping("/dashboard/revenue")
    @Operation(
            summary = "Get revenue KPIs",
            description =
                    "Gross revenue sums SUCCESS and REFUNDED payments by completedAt. Refund amount includes only SUCCEEDED refunds by processedAt. Net may be negative. Results are currency-separated and zero-valued for a requested currency with no rows.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Report generated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Validation failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Access denied")
    })
    public ResponseEntity<ApiResponse<AdminRevenueKpiResponse>> revenue(
            @Parameter(required = true, example = "2026-07-01")
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate startDate,
            @Parameter(required = true, example = "2026-07-31")
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate endDate,
            @Parameter(example = "NPR") @RequestParam(required = false) String currency) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Revenue KPI report fetched successfully",
                        revenue.getKpis(range(startDate, endDate), currency)));
    }

    @GetMapping("/revenue")
    @Operation(
            summary = "Get detailed revenue trends",
            description =
                    "Returns complete DAY, ISO WEEK, or MONTH buckets. SUCCESS and REFUNDED payments are attributed by completedAt; SUCCEEDED refunds by processedAt. Empty buckets and currencies remain separate.")
    public ResponseEntity<ApiResponse<DetailedRevenueReportResponse>> detailedRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String currency,
            @RequestParam(defaultValue = "DAY") ReportGrouping groupBy) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Detailed revenue report fetched successfully",
                        revenue.getDetailedReport(range(startDate, endDate), currency, groupBy)));
    }

    @GetMapping("/bookings")
    @Operation(
            summary = "Get detailed booking trends",
            description =
                    "Returns complete DAY, ISO WEEK, or MONTH buckets attributed by bookingTime, including every booking status and zero-valued empty buckets.")
    public ResponseEntity<ApiResponse<DetailedBookingReportResponse>> detailedBookings(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "DAY") ReportGrouping groupBy) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Detailed booking report fetched successfully",
                        bookings.getDetailedReport(range(startDate, endDate), groupBy)));
    }

    @GetMapping("/occupancy")
    @Operation(
            summary = "Get show occupancy",
            description =
                    "Uses distinct seats on CONFIRMED bookings divided by generated show seats. Cancelled shows are excluded unless explicitly requested; zero-capacity rows are marked unmeasurable.")
    public ResponseEntity<ApiResponse<OccupancySummaryResponse>> occupancy(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) Long hallId,
            @RequestParam(required = false)
                    com.chalchitraghar.modules.shows.enums.ShowStatus showStatus,
            @RequestParam(defaultValue = "SHOW_DATE") OccupancySort sort,
            @RequestParam(defaultValue = "DESC") ReportSortDirection direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Occupancy report fetched successfully",
                        occupancy.getReport(
                                range(startDate, endDate),
                                movieId,
                                hallId,
                                showStatus,
                                sort,
                                direction,
                                page,
                                size)));
    }

    @GetMapping("/movies")
    @Operation(
            summary = "Get movie performance",
            description =
                    "Show-date-filtered movie performance with independently aggregated currency values and stable database pagination.")
    public ResponseEntity<
                    ApiResponse<
                            com.chalchitraghar.shared.response.PageResponse<
                                    MoviePerformanceResponse>>>
            movies(
                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                            LocalDate startDate,
                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                    @RequestParam(required = false) String currency,
                    @RequestParam(defaultValue = "REVENUE") MoviePerformanceSort sort,
                    @RequestParam(defaultValue = "DESC") ReportSortDirection direction,
                    @RequestParam(defaultValue = "0") int page,
                    @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Movie performance report fetched successfully",
                        performance.movies(
                                range(startDate, endDate), currency, sort, direction, page, size)));
    }

    @GetMapping("/halls")
    @Operation(
            summary = "Get hall performance",
            description =
                    "Show-date-filtered hall performance using generated show seats rather than configured hall capacity for occupancy.")
    public ResponseEntity<
                    ApiResponse<
                            com.chalchitraghar.shared.response.PageResponse<
                                    HallPerformanceResponse>>>
            halls(
                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                            LocalDate startDate,
                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                    @RequestParam(required = false) String currency,
                    @RequestParam(defaultValue = "REVENUE") HallPerformanceSort sort,
                    @RequestParam(defaultValue = "DESC") ReportSortDirection direction,
                    @RequestParam(defaultValue = "0") int page,
                    @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Hall performance report fetched successfully",
                        performance.halls(
                                range(startDate, endDate), currency, sort, direction, page, size)));
    }

    @GetMapping("/shows")
    @Operation(
            summary = "Get show performance",
            description =
                    "One stable, paginated row per show with independently aggregated financial values and generated-seat occupancy.")
    public ResponseEntity<
                    ApiResponse<
                            com.chalchitraghar.shared.response.PageResponse<
                                    ShowPerformanceResponse>>>
            shows(
                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                            LocalDate startDate,
                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                    @RequestParam(required = false) Long movieId,
                    @RequestParam(required = false) Long hallId,
                    @RequestParam(required = false)
                            com.chalchitraghar.modules.shows.enums.ShowStatus status,
                    @RequestParam(required = false) String currency,
                    @RequestParam(defaultValue = "SHOW_DATE") ShowPerformanceSort sort,
                    @RequestParam(defaultValue = "DESC") ReportSortDirection direction,
                    @RequestParam(defaultValue = "0") int page,
                    @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Show performance report fetched successfully",
                        performance.shows(
                                range(startDate, endDate),
                                movieId,
                                hallId,
                                status,
                                currency,
                                sort,
                                direction,
                                page,
                                size)));
    }

    private ReportingDateRange range(LocalDate startDate, LocalDate endDate) {
        return ReportingDateRange.of(startDate, endDate, clock);
    }
}
