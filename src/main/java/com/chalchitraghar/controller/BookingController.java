package com.chalchitraghar.controller;

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
import com.chalchitraghar.service.BookingService;
import com.chalchitraghar.service.SeatLockService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for booking-related endpoints.
 * Requires authentication for all operations.
 */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final SeatLockService seatLockService;
    private final BookingService bookingService;

    /**
     * Validates and locks seats for booking.
     * This endpoint requires JWT authentication and is accessible to authenticated users.
     *
     * @param request contains showId and list of seatIds to validate and lock
     * @return success response with validation details
     */
    @PostMapping("/validate")
    public ResponseEntity<BookingValidationResponse> validateAndLockSeats(
            @Valid @RequestBody BookingValidationRequest request) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new com.chalchitraghar.exception.AuthenticationException("User not authenticated");
        }
        
        User currentUser = (User) authentication.getPrincipal();
        
        // Validate and lock seats
        seatLockService.validateAndLockSeats(
                request.getShowId(),
                request.getSeatIds(),
                currentUser.getId()
        );
        
        BookingValidationResponse response = new BookingValidationResponse(
                "Seats validated and locked successfully",
                request.getShowId(),
                request.getSeatIds().size()
        );
        
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Creates a new booking for the authenticated customer.
     * This endpoint requires JWT authentication and is accessible only to users with CUSTOMER role.
     *
     * @param request contains showId and list of seatIds to book
     * @return created booking response with booking details
     */
    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody BookingRequest request) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new com.chalchitraghar.exception.AuthenticationException("User not authenticated");
        }
        
        User currentUser = (User) authentication.getPrincipal();
        
        // Create booking
        BookingResponse response = bookingService.createBooking(request, currentUser);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Confirms a booking and permanently locks the associated seats.
     * This endpoint requires JWT authentication and is accessible only to users with CUSTOMER role.
     * Only the booking owner can confirm their booking.
     *
     * @param bookingId the ID of the booking to confirm
     * @return confirmed booking response with updated status
     */
    @PostMapping("/{bookingId}/confirm")
    public ResponseEntity<BookingResponse> confirmBooking(@PathVariable Long bookingId) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new com.chalchitraghar.exception.AuthenticationException("User not authenticated");
        }
        
        User currentUser = (User) authentication.getPrincipal();
        
        // Confirm booking
        BookingResponse response = bookingService.confirmBooking(bookingId, currentUser);
        
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Retrieves all bookings for the authenticated user.
     * This endpoint requires JWT authentication and is accessible only to users with CUSTOMER role.
     * Returns bookings sorted by booking time descending (most recent first).
     *
     * @return list of booking responses for the authenticated user
     */
    @GetMapping("/my")
    public ResponseEntity<List<BookingResponse>> getMyBookings() {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new com.chalchitraghar.exception.AuthenticationException("User not authenticated");
        }
        
        User currentUser = (User) authentication.getPrincipal();
        
        // Get all bookings for the authenticated user
        List<BookingResponse> bookings = bookingService.getMyBookings(currentUser);
        
        return ResponseEntity.status(HttpStatus.OK).body(bookings);
    }
}
