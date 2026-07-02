package com.chalchitraghar.applications.admin;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chalchitraghar.modules.users.dto.response.AdminUserDetailResponse;
import com.chalchitraghar.modules.users.dto.response.AdminUserSummaryResponse;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.modules.users.mapper.UserMapper;
import com.chalchitraghar.modules.users.service.UserService;
import com.chalchitraghar.shared.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
    private final UserMapper userMapper;

    @GetMapping
    @Operation(
            summary = "List all users",
            description = "Returns admin user summaries without password, Google subject ID, or OTP data.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Users fetched successfully",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "message": "Users fetched successfully",
                      "data": [
                        {
                          "id": 1,
                          "name": "Aarav Sharma",
                          "email": "aarav@example.com",
                          "role": "CUSTOMER",
                          "enabled": true,
                          "locked": false
                        }
                      ],
                      "errors": []
                    }
                    """)))
    public ResponseEntity<ApiResponse<List<AdminUserSummaryResponse>>> getAllUsers() {
        List<AdminUserSummaryResponse> users = userService.getAllUsers().stream()
                .map(userMapper::toAdminSummaryResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Users fetched successfully", users));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get user by id",
            description = "Returns admin user details without password, Google subject ID, or OTP data.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "User fetched successfully",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "message": "User fetched successfully",
                      "data": {
                        "id": 1,
                        "name": "Aarav Sharma",
                        "email": "aarav@example.com",
                        "role": "CUSTOMER",
                        "authProvider": "LOCAL",
                        "emailVerified": false,
                        "enabled": true,
                        "locked": false,
                        "failedLoginAttempts": 0,
                        "lockedUntil": null,
                        "lastLoginAt": "2026-07-02T10:15:30",
                        "passwordChangedAt": "2026-07-01T09:00:00",
                        "createdAt": "2026-07-01T09:00:00",
                        "updatedAt": "2026-07-02T10:15:30"
                      },
                      "errors": []
                    }
                    """)))
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(
                "User fetched successfully",
                userMapper.toAdminDetailResponse(user)));
    }
}
