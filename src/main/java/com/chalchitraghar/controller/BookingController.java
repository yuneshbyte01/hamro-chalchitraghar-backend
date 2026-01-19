package com.chalchitraghar.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chalchitraghar.dto.booking.BookingValidationRequestDto;
import com.chalchitraghar.dto.booking.BookingValidationResponseDto;
import com.chalchitraghar.model.User;
import com.chalchitraghar.service.BookingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    /**
     * Validates and locks seats for booking.
     * This endpoint requires JWT authentication and is accessible to authenticated users.
     * 
     * @param requestDto Contains showId and list of seatIds to validate and lock
     * @return Success response with validation details
     */
    @PostMapping("/validate")
    public ResponseEntity<BookingValidationResponseDto> validateAndLockSeats(
            @Valid @RequestBody BookingValidationRequestDto requestDto) {
        
        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new com.chalchitraghar.exception.AuthenticationException("User not authenticated");
        }
        
        User currentUser = (User) authentication.getPrincipal();
        
        // Validate and lock seats
        bookingService.validateAndLockSeats(
                requestDto.getShowId(),
                requestDto.getSeatIds(),
                currentUser.getId()
        );
        
        // Build success response
        BookingValidationResponseDto response = new BookingValidationResponseDto(
                "Seats validated and locked successfully",
                requestDto.getShowId(),
                requestDto.getSeatIds().size()
        );
        
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
