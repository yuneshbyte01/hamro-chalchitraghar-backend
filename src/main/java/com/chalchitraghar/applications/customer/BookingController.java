package com.chalchitraghar.applications.customer;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;

import com.chalchitraghar.modules.bookings.dto.request.BookingRequest;
import com.chalchitraghar.modules.bookings.dto.request.BookingSearchCriteria;
import com.chalchitraghar.modules.bookings.dto.response.CustomerBookingDetailResponse;
import com.chalchitraghar.modules.bookings.dto.response.CustomerBookingSummaryResponse;
import com.chalchitraghar.modules.bookings.dto.request.SeatHoldRequest;
import com.chalchitraghar.modules.bookings.dto.response.SeatHoldResponse;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.modules.bookings.service.BookingService;
import com.chalchitraghar.modules.seats.service.SeatLockService;
import com.chalchitraghar.shared.response.ApiResponse;
import com.chalchitraghar.shared.response.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for booking endpoints. Authenticated users only. No admin-only logic.
 */
@RestController
@RequestMapping("/api/customer/bookings")
@RequiredArgsConstructor
@Tag(name = "Customer Bookings", description = "Customer seat hold and booking endpoints")
@SecurityRequirement(name = "bearerAuth")
public class BookingController {

    private final SeatLockService seatLockService;
    private final BookingService bookingService;

    @PostMapping("/hold")
    @Operation(summary = "Hold seats", description = "Locks available seats for the current user before booking.")
    public ResponseEntity<ApiResponse<SeatHoldResponse>> holdSeats(@Valid @RequestBody SeatHoldRequest request) {
        User currentUser = getCurrentUser();
        SeatHoldResponse response = seatLockService.holdSeats(request.getShowId(), request.getSeatIds(), currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Seats held successfully", response));
    }

    @PostMapping
    @Operation(summary = "Create a booking", description = "Creates an INITIATED booking from available seats or seats held by the current user.")
    public ResponseEntity<ApiResponse<CustomerBookingDetailResponse>> createBooking(@Valid @RequestBody BookingRequest request) {
        User currentUser = getCurrentUser();
        CustomerBookingDetailResponse response = bookingService.createBooking(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking created successfully", response));
    }

    @PostMapping("/{bookingId}/confirm")
    @Operation(summary = "Confirm a booking", description = "Confirms an INITIATED booking owned by the current user and marks seats BOOKED.")
    public ResponseEntity<ApiResponse<CustomerBookingDetailResponse>> confirmBooking(@PathVariable Long bookingId) {
        User currentUser = getCurrentUser();
        CustomerBookingDetailResponse response = bookingService.confirmBooking(bookingId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Booking confirmed successfully", response));
    }

    @GetMapping("/my")
    @Operation(summary = "List my bookings", description = "Authenticated owner-only paginated booking history.")
    public ResponseEntity<ApiResponse<PageResponse<CustomerBookingSummaryResponse>>> getMyBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "bookingTime") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate showDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate showDateTo) {
        User currentUser = getCurrentUser();
        BookingSearchCriteria criteria = new BookingSearchCriteria(
                null, status, null, null, null, null, showDateFrom, showDateTo, null, null);
        return ResponseEntity.ok(ApiResponse.success("Bookings fetched successfully",
                bookingService.getCustomerBookings(currentUser, criteria, page, size, sortBy, sortDir)));
    }

    @GetMapping("/{bookingId}")
    @Operation(summary = "Get a booking by ID")
    public ResponseEntity<ApiResponse<CustomerBookingDetailResponse>> getBookingById(@PathVariable Long bookingId) {
        User currentUser = getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("Booking fetched successfully", bookingService.getCustomerBookingById(bookingId, currentUser)));
    }

    @GetMapping("/reference/{bookingReference}")
    @Operation(summary = "Get my booking by reference", description = "Owner-only lookup; non-owner references are hidden as not found.")
    public ResponseEntity<ApiResponse<CustomerBookingDetailResponse>> getBookingByReference(
            @PathVariable String bookingReference) {
        return ResponseEntity.ok(ApiResponse.success("Booking fetched successfully",
                bookingService.getCustomerBookingByReference(bookingReference, getCurrentUser())));
    }

    @PostMapping("/{bookingId}/cancel")
    @Operation(summary = "Cancel a booking", description = "Cancels an INITIATED booking owned by the current user and releases reserved seats.")
    public ResponseEntity<ApiResponse<CustomerBookingDetailResponse>> cancelBooking(@PathVariable Long bookingId) {
        User currentUser = getCurrentUser();
        CustomerBookingDetailResponse response = bookingService.cancelBooking(bookingId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully", response));
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new com.chalchitraghar.shared.exception.AuthenticationException("User not authenticated");
        }
        return (User) auth.getPrincipal();
    }
}
