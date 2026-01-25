package com.chalchitraghar.controller.booking;

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

import com.chalchitraghar.dto.booking.BookingRequest;
import com.chalchitraghar.dto.booking.BookingResponse;
import com.chalchitraghar.dto.booking.BookingValidationRequest;
import com.chalchitraghar.dto.booking.BookingValidationResponse;
import com.chalchitraghar.model.User;
import com.chalchitraghar.service.booking.BookingService;
import com.chalchitraghar.service.booking.SeatLockService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for booking endpoints. Authenticated users only. No admin-only logic.
 */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final SeatLockService seatLockService;
    private final BookingService bookingService;

    @PostMapping("/validate")
    public ResponseEntity<BookingValidationResponse> validateAndLockSeats(
            @Valid @RequestBody BookingValidationRequest request) {
        User currentUser = getCurrentUser();
        seatLockService.validateAndLockSeats(request.getShowId(), request.getSeatIds(), currentUser.getId());
        BookingValidationResponse response = new BookingValidationResponse(
                "Seats validated and locked successfully",
                request.getShowId(),
                request.getSeatIds().size()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingRequest request) {
        User currentUser = getCurrentUser();
        BookingResponse response = bookingService.createBooking(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{bookingId}/confirm")
    public ResponseEntity<BookingResponse> confirmBooking(@PathVariable Long bookingId) {
        User currentUser = getCurrentUser();
        BookingResponse response = bookingService.confirmBooking(bookingId, currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<BookingResponse>> getMyBookings() {
        User currentUser = getCurrentUser();
        return ResponseEntity.ok(bookingService.getMyBookings(currentUser));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable Long bookingId) {
        User currentUser = getCurrentUser();
        return ResponseEntity.ok(bookingService.getBookingById(bookingId, currentUser));
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(@PathVariable Long bookingId) {
        User currentUser = getCurrentUser();
        BookingResponse response = bookingService.cancelBooking(bookingId, currentUser);
        return ResponseEntity.ok(response);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new com.chalchitraghar.exception.AuthenticationException("User not authenticated");
        }
        return (User) auth.getPrincipal();
    }
}
