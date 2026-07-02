package com.chalchitraghar.applications.customer;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chalchitraghar.modules.users.dto.request.ChangePasswordRequest;
import com.chalchitraghar.modules.users.dto.request.ProfileUpdateRequest;
import com.chalchitraghar.modules.users.dto.response.UserResponse;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.modules.users.mapper.UserMapper;
import com.chalchitraghar.modules.users.service.UserService;
import com.chalchitraghar.shared.exception.AuthenticationException;
import com.chalchitraghar.shared.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for authenticated customer profile details.
 */
@RestController
@RequestMapping("/api/customer/profile")
@RequiredArgsConstructor
@Tag(name = "Customer Profile", description = "Authenticated customer profile endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping
    @Operation(
            summary = "Get current customer profile",
            description = "Returns the authenticated user's profile without password details.")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile() {
        User currentUser = getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(
                "Profile fetched successfully",
                userMapper.toResponseDto(currentUser)));
    }

    @PutMapping
    @Operation(
            summary = "Update current customer profile",
            description = "Updates only the authenticated user's display name. Email and role cannot be changed here.")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody ProfileUpdateRequest request) {
        User updatedUser = userService.updateCurrentUserProfile(getCurrentUser(), request.getName());
        return ResponseEntity.ok(ApiResponse.success(
                "Profile updated successfully",
                userMapper.toResponseDto(updatedUser)));
    }

    @PutMapping("/password")
    @Operation(
            summary = "Change current customer password",
            description = "Verifies the current password, validates the new strong password, and stores it as a BCrypt hash.")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changeCurrentUserPassword(
                getCurrentUser(),
                request.getCurrentPassword(),
                request.getNewPassword()
        );
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new AuthenticationException("User not authenticated");
        }
        return (User) auth.getPrincipal();
    }
}
