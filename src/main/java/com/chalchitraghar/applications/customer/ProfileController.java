package com.chalchitraghar.applications.customer;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chalchitraghar.modules.users.dto.response.UserResponse;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.shared.exception.AuthenticationException;
import com.chalchitraghar.shared.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller for authenticated customer profile details.
 */
@RestController
@RequestMapping("/api/customer/profile")
@Tag(name = "Customer Bookings", description = "Authenticated customer profile endpoint")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    @GetMapping
    @Operation(summary = "Get current customer profile")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile() {
        User currentUser = getCurrentUser();
        UserResponse response = new UserResponse(
                currentUser.getId(),
                currentUser.getName(),
                currentUser.getEmail(),
                currentUser.getRole().name()
        );
        return ResponseEntity.ok(ApiResponse.success("Profile fetched successfully", response));
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new AuthenticationException("User not authenticated");
        }
        return (User) auth.getPrincipal();
    }
}
