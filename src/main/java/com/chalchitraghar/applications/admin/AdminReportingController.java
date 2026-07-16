package com.chalchitraghar.applications.admin;

import com.chalchitraghar.modules.reporting.dto.request.ReportingDateRange;
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
@RequestMapping("/api/admin/reports/dashboard")
@RequiredArgsConstructor
@Tag(name = "Admin Reports", description = "ADMIN-only business dashboard and core KPI reports")
@SecurityRequirement(name = "bearerAuth")
public class AdminReportingController {
    private final DashboardReportService dashboard;
    private final BookingReportService bookings;
    private final RevenueReportService revenue;
    private final Clock clock;

    @GetMapping
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

    @GetMapping("/bookings")
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

    @GetMapping("/revenue")
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

    private ReportingDateRange range(LocalDate startDate, LocalDate endDate) {
        return ReportingDateRange.of(startDate, endDate, clock);
    }
}
