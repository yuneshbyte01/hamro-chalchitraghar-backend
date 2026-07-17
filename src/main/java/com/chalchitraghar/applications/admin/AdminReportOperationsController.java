package com.chalchitraghar.applications.admin;

import com.chalchitraghar.modules.reporting.comparison.ReportingComparisonService;
import com.chalchitraghar.modules.reporting.comparison.dto.*;
import com.chalchitraghar.modules.reporting.dto.request.*;
import com.chalchitraghar.modules.reporting.enums.*;
import com.chalchitraghar.modules.reporting.export.*;
import com.chalchitraghar.modules.reporting.schedule.ScheduledReportService;
import com.chalchitraghar.modules.reporting.schedule.dto.*;
import com.chalchitraghar.modules.shows.enums.ShowStatus;
import com.chalchitraghar.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@Tag(
        name = "Admin Reports",
        description = "ADMIN-only reports, exports, comparisons, and schedules")
@SecurityRequirement(name = "bearerAuth")
public class AdminReportOperationsController {
    private final ReportingExportService exports;
    private final ReportingComparisonService comparisons;
    private final ScheduledReportService schedules;
    private final Clock clock;

    @GetMapping("/exports/revenue")
    @Operation(summary = "Export revenue as formula-safe CSV or XLSX")
    public ResponseEntity<byte[]> revenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String currency,
            @RequestParam(defaultValue = "DAY") ReportGrouping groupBy,
            @RequestParam ReportExportFormat format) {
        return download(exports.revenue(range(startDate, endDate), currency, groupBy, format));
    }

    @GetMapping("/exports/bookings")
    @Operation(summary = "Export booking trends")
    public ResponseEntity<byte[]> bookings(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "DAY") ReportGrouping groupBy,
            @RequestParam ReportExportFormat format) {
        return download(exports.bookings(range(startDate, endDate), groupBy, format));
    }

    @GetMapping("/exports/occupancy")
    @Operation(summary = "Export complete filtered occupancy rows")
    public ResponseEntity<byte[]> occupancy(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) Long hallId,
            @RequestParam(required = false) ShowStatus showStatus,
            @RequestParam(defaultValue = "SHOW_DATE") OccupancySort sort,
            @RequestParam(defaultValue = "DESC") ReportSortDirection direction,
            @RequestParam ReportExportFormat format) {
        return download(
                exports.occupancy(
                        range(startDate, endDate),
                        movieId,
                        hallId,
                        showStatus,
                        sort,
                        direction,
                        format));
    }

    @GetMapping("/exports/movies")
    @Operation(summary = "Export movie performance with one row per movie and currency")
    public ResponseEntity<byte[]> movies(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String currency,
            @RequestParam(defaultValue = "REVENUE") MoviePerformanceSort sort,
            @RequestParam(defaultValue = "DESC") ReportSortDirection direction,
            @RequestParam ReportExportFormat format) {
        return download(
                exports.movies(range(startDate, endDate), currency, sort, direction, format));
    }

    @GetMapping("/exports/halls")
    @Operation(summary = "Export hall performance with one row per hall and currency")
    public ResponseEntity<byte[]> halls(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String currency,
            @RequestParam(defaultValue = "REVENUE") HallPerformanceSort sort,
            @RequestParam(defaultValue = "DESC") ReportSortDirection direction,
            @RequestParam ReportExportFormat format) {
        return download(
                exports.halls(range(startDate, endDate), currency, sort, direction, format));
    }

    @GetMapping("/exports/shows")
    @Operation(summary = "Export show performance with one row per show and currency")
    public ResponseEntity<byte[]> shows(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) Long hallId,
            @RequestParam(required = false) ShowStatus status,
            @RequestParam(required = false) String currency,
            @RequestParam(defaultValue = "SHOW_DATE") ShowPerformanceSort sort,
            @RequestParam(defaultValue = "DESC") ReportSortDirection direction,
            @RequestParam ReportExportFormat format) {
        return download(
                exports.shows(
                        range(startDate, endDate),
                        movieId,
                        hallId,
                        status,
                        currency,
                        sort,
                        direction,
                        format));
    }

    @GetMapping("/comparisons/periods")
    @Operation(summary = "Compare a period with the same-length previous period or a custom period")
    public ResponseEntity<ApiResponse<PeriodComparisonResponse>> periods(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate currentStartDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate currentEndDate,
            @RequestParam(defaultValue = "PREVIOUS_PERIOD") ComparisonMode comparisonMode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate previousStartDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate previousEndDate,
            @RequestParam(required = false) String currency) {
        return ok(
                "Period comparison fetched successfully",
                comparisons.periods(
                        range(currentStartDate, currentEndDate),
                        comparisonMode,
                        previousStartDate,
                        previousEndDate,
                        currency));
    }

    @GetMapping("/comparisons/movies")
    @Operation(summary = "Compare two to five movies with currency-safe rankings")
    public ResponseEntity<ApiResponse<List<MovieComparisonResponse>>> compareMovies(
            @RequestParam List<Long> movieIds,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String currency) {
        return ok(
                "Movie comparison fetched successfully",
                comparisons.movies(range(startDate, endDate), movieIds, currency));
    }

    @GetMapping("/comparisons/halls")
    @Operation(summary = "Compare two to five halls with currency-safe rankings")
    public ResponseEntity<ApiResponse<List<HallComparisonResponse>>> compareHalls(
            @RequestParam List<Long> hallIds,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String currency) {
        return ok(
                "Hall comparison fetched successfully",
                comparisons.halls(range(startDate, endDate), hallIds, currency));
    }

    @PostMapping("/schedules")
    @Operation(summary = "Create a scheduled report")
    public ResponseEntity<ApiResponse<ScheduledReportResponse>> create(
            @Valid @RequestBody ScheduledReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Scheduled report created", schedules.create(request)));
    }

    @GetMapping("/schedules")
    @Operation(summary = "List scheduled reports")
    public ResponseEntity<ApiResponse<List<ScheduledReportResponse>>> list() {
        return ok("Scheduled reports fetched", schedules.list());
    }

    @GetMapping("/schedules/{id}")
    @Operation(summary = "Get a scheduled report")
    public ResponseEntity<ApiResponse<ScheduledReportResponse>> get(@PathVariable long id) {
        return ok("Scheduled report fetched", schedules.get(id));
    }

    @PutMapping("/schedules/{id}")
    @Operation(summary = "Update a scheduled report")
    public ResponseEntity<ApiResponse<ScheduledReportResponse>> update(
            @PathVariable long id, @Valid @RequestBody ScheduledReportRequest request) {
        return ok("Scheduled report updated", schedules.update(id, request));
    }

    @PostMapping("/schedules/{id}/enable")
    @Operation(summary = "Enable a scheduled report")
    public ResponseEntity<ApiResponse<ScheduledReportResponse>> enable(@PathVariable long id) {
        return ok("Scheduled report enabled", schedules.enable(id, true));
    }

    @PostMapping("/schedules/{id}/disable")
    @Operation(summary = "Disable a scheduled report")
    public ResponseEntity<ApiResponse<ScheduledReportResponse>> disable(@PathVariable long id) {
        return ok("Scheduled report disabled", schedules.enable(id, false));
    }

    @DeleteMapping("/schedules/{id}")
    @Operation(summary = "Soft-delete a scheduled report while preserving delivery history")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        schedules.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/schedules/{id}/run")
    @Operation(summary = "Idempotently generate and deliver the latest completed period")
    public ResponseEntity<ApiResponse<ReportDeliveryResponse>> run(@PathVariable long id) {
        return ok("Scheduled report run completed", schedules.run(id));
    }

    private ReportingDateRange range(LocalDate start, LocalDate end) {
        return ReportingDateRange.of(start, end, clock);
    }

    private ResponseEntity<byte[]> download(ReportExportResult result) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.contentType()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + result.fileName() + "\"")
                .header("X-Report-Rows", Integer.toString(result.rowCount()))
                .body(result.content());
    }

    private <T> ResponseEntity<ApiResponse<T>> ok(String message, T data) {
        return ResponseEntity.ok(ApiResponse.success(message, data));
    }
}
