package com.chalchitraghar.applications.staff;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chalchitraghar.modules.bookings.dto.response.BookingResponse;
import com.chalchitraghar.modules.bookings.service.BookingService;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.shared.exception.AuthenticationException;
import com.chalchitraghar.shared.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for staff booking lookup.
 */
@RestController
@RequestMapping("/api/staff/bookings")
@RequiredArgsConstructor
@Tag(name = "Staff", description = "Staff booking lookup endpoints")
@SecurityRequirement(name = "bearerAuth")
public class BookingManagementController {

    private final BookingService bookingService;

    @GetMapping("/{bookingId}")
    @Operation(summary = "Get booking by ID for staff")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(@PathVariable Long bookingId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Booking fetched successfully",
                bookingService.getBookingById(bookingId, getCurrentUser())));
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new AuthenticationException("User not authenticated");
        }
        return (User) auth.getPrincipal();
    }
}
