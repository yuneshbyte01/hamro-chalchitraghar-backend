package com.chalchitraghar.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chalchitraghar.dto.booking.BookingValidationRequest;
import com.chalchitraghar.dto.booking.BookingValidationResponse;
import com.chalchitraghar.model.User;
import com.chalchitraghar.service.BookingService;

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
        bookingService.validateAndLockSeats(
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
}
