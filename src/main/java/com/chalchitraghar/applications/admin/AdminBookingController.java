package com.chalchitraghar.applications.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;

import com.chalchitraghar.modules.bookings.dto.request.BookingSearchCriteria;
import com.chalchitraghar.modules.bookings.dto.response.AdminBookingDetailResponse;
import com.chalchitraghar.modules.bookings.dto.response.AdminBookingSummaryResponse;
import com.chalchitraghar.modules.bookings.service.BookingService;
import com.chalchitraghar.shared.response.ApiResponse;
import com.chalchitraghar.shared.response.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/bookings")
@RequiredArgsConstructor
@Tag(name = "Admin Bookings", description = "Admin booking management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminBookingController {

    private final BookingService bookingService;

    @GetMapping
    @Operation(summary = "List bookings for admins", description = "Paginated operational search across safe customer, movie, and hall fields.")
    public ResponseEntity<ApiResponse<PageResponse<AdminBookingSummaryResponse>>> getBookings(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "bookingTime") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search, @RequestParam(required = false) String status,
            @RequestParam(required = false) Long showId, @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) Long hallId, @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate showDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate showDateTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime bookingTimeFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime bookingTimeTo) {
        BookingSearchCriteria criteria = new BookingSearchCriteria(search, status, showId, movieId, hallId,
                customerId, showDateFrom, showDateTo, bookingTimeFrom, bookingTimeTo);
        return ResponseEntity.ok(ApiResponse.success("Bookings fetched successfully",
                bookingService.getAdminBookings(criteria, page, size, sortBy, sortDir)));
    }

    @GetMapping("/{bookingId}")
    @Operation(summary = "Get any booking by ID", description = "Requires ADMIN role and returns safe operational booking details.")
    public ResponseEntity<ApiResponse<AdminBookingDetailResponse>> getBookingById(@PathVariable Long bookingId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Booking fetched successfully", bookingService.getAdminBookingById(bookingId)));
    }
}
