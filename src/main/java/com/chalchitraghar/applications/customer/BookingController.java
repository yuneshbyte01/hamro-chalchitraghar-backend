package com.chalchitraghar.applications.customer;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chalchitraghar.modules.bookings.dto.request.BookingRequest;
import com.chalchitraghar.modules.bookings.dto.response.BookingResponse;
import com.chalchitraghar.modules.bookings.dto.request.SeatHoldRequest;
import com.chalchitraghar.modules.bookings.dto.response.SeatHoldResponse;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.modules.bookings.service.BookingService;
import com.chalchitraghar.modules.seats.service.SeatLockService;
import com.chalchitraghar.shared.response.ApiResponse;

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
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(@Valid @RequestBody BookingRequest request) {
        User currentUser = getCurrentUser();
        BookingResponse response = bookingService.createBooking(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking created successfully", response));
    }

    @PostMapping("/{bookingId}/confirm")
    @Operation(summary = "Confirm a booking", description = "Confirms an INITIATED booking owned by the current user and marks seats BOOKED.")
    public ResponseEntity<ApiResponse<BookingResponse>> confirmBooking(@PathVariable Long bookingId) {
        User currentUser = getCurrentUser();
        BookingResponse response = bookingService.confirmBooking(bookingId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Booking confirmed successfully", response));
    }

    @GetMapping("/my")
    @Operation(summary = "List my bookings")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyBookings() {
        User currentUser = getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("Bookings fetched successfully", bookingService.getMyBookings(currentUser)));
    }

    @GetMapping("/{bookingId}")
    @Operation(summary = "Get a booking by ID")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(@PathVariable Long bookingId) {
        User currentUser = getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("Booking fetched successfully", bookingService.getBookingById(bookingId, currentUser)));
    }

    @PostMapping("/{bookingId}/cancel")
    @Operation(summary = "Cancel a booking", description = "Cancels an INITIATED booking owned by the current user and releases reserved seats.")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(@PathVariable Long bookingId) {
        User currentUser = getCurrentUser();
        BookingResponse response = bookingService.cancelBooking(bookingId, currentUser);
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
