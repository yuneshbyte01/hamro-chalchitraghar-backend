package com.chalchitraghar.applications.admin;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chalchitraghar.modules.users.dto.response.UserResponse;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.modules.users.service.UserService;
import com.chalchitraghar.shared.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for admin user management. Admin only.
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin Users", description = "Admin user lookup endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "List all users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Users fetched successfully", users));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by id")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User fetched successfully", toResponse(user)));
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole().name());
    }
}
