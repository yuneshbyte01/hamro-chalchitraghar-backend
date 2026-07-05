package com.chalchitraghar.applications.admin;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chalchitraghar.modules.users.dto.request.AdminCreateUserRequest;
import com.chalchitraghar.modules.users.dto.request.AdminUpdateUserRequest;
import com.chalchitraghar.modules.users.dto.request.AdminUserSearchCriteria;
import com.chalchitraghar.modules.users.dto.response.AdminUserDetailResponse;
import com.chalchitraghar.modules.users.dto.response.AdminUserSummaryResponse;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.modules.users.mapper.UserMapper;
import com.chalchitraghar.modules.users.service.UserService;
import com.chalchitraghar.shared.exception.AuthenticationException;
import com.chalchitraghar.shared.response.ApiResponse;
import com.chalchitraghar.shared.response.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
                      "data": {
                        "content": [
                          {
                            "id": 1,
                            "name": "Aarav Sharma",
                            "email": "aarav@example.com",
                            "role": "CUSTOMER",
                            "enabled": true,
                            "locked": false
                          }
                        ],
                        "page": 0,
                        "size": 20,
                        "totalElements": 1,
                        "totalPages": 1,
                        "last": true
                      },
                      "errors": []
                    }
                    """)))
    public ResponseEntity<ApiResponse<PageResponse<AdminUserSummaryResponse>>> getAllUsers(
            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field: id, name, email, role, enabled, locked, authProvider, createdAt, updatedAt, lastLoginAt")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction: asc or desc")
            @RequestParam(defaultValue = "desc") String sortDir,
            @Parameter(description = "Case-insensitive search term matched against name and email")
            @RequestParam(required = false) String search,
            @Parameter(description = "Filter by role: CUSTOMER, STAFF, ADMIN")
            @RequestParam(required = false) String role,
            @Parameter(description = "Filter by auth provider: LOCAL, GOOGLE")
            @RequestParam(required = false) String authProvider,
            @Parameter(description = "Filter by enabled account status")
            @RequestParam(required = false) Boolean enabled,
            @Parameter(description = "Filter by locked account status")
            @RequestParam(required = false) Boolean locked,
            @Parameter(description = "Filter by email verification status")
            @RequestParam(required = false) Boolean emailVerified) {
        PageResponse<AdminUserSummaryResponse> users = userService.getAdminUsers(
                new AdminUserSearchCriteria(search, role, authProvider, enabled, locked, emailVerified),
                page,
                size,
                sortBy,
                sortDir);
        return ResponseEntity.ok(ApiResponse.success("Users fetched successfully", users));
    }

    @PostMapping
    @Operation(
            summary = "Create internal user",
            description = "Creates a LOCAL CUSTOMER, STAFF, or ADMIN account with a BCrypt password.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {
                      "name": "John Doe",
                      "email": "john@example.com",
                      "password": "StrongPass@123",
                      "role": "STAFF"
                    }
                    """)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "User created successfully",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = ADMIN_ACCOUNT_STATE_EXAMPLE)))
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> createUser(
            @Valid @RequestBody AdminCreateUserRequest request) {
        User user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "User created successfully",
                userMapper.toAdminDetailResponse(user)));
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

    @PutMapping("/{id}")
    @Operation(
            summary = "Update user lifecycle fields",
            description = "Updates allowed admin-managed fields: name, role, enabled, and emailVerified.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {
                      "name": "John Doe",
                      "role": "ADMIN",
                      "enabled": true,
                      "emailVerified": false
                    }
                    """)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "User updated successfully",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = ADMIN_ACCOUNT_STATE_EXAMPLE)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid role or administrative safeguard violation",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "message": "You cannot remove your own ADMIN role",
                      "data": null,
                      "errors": []
                    }
                    """)))
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateUserRequest request) {
        User user = userService.updateUser(id, request, getCurrentUser());
        return ResponseEntity.ok(ApiResponse.success(
                "User updated successfully",
                userMapper.toAdminDetailResponse(user)));
    }

    @PutMapping("/{id}/enable")
    @Operation(
            summary = "Enable user account",
            description = "Sets enabled=true for the selected user and returns updated admin user details.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "User enabled successfully",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = ADMIN_ACCOUNT_STATE_EXAMPLE)))
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> enableUser(@PathVariable Long id) {
        User user = userService.enableUser(id, getCurrentUser());
        return ResponseEntity.ok(ApiResponse.success(
                "User enabled successfully",
                userMapper.toAdminDetailResponse(user)));
    }

    @PutMapping("/{id}/disable")
    @Operation(
            summary = "Disable user account",
            description = "Sets enabled=false for the selected user. Admins cannot disable their own account.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "User disabled successfully",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = ADMIN_ACCOUNT_STATE_EXAMPLE)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Self-disable rejected",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "message": "You cannot disable your own account",
                      "data": null,
                      "errors": []
                    }
                    """)))
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> disableUser(@PathVariable Long id) {
        User user = userService.disableUser(id, getCurrentUser());
        return ResponseEntity.ok(ApiResponse.success(
                "User disabled successfully",
                userMapper.toAdminDetailResponse(user)));
    }

    @PutMapping("/{id}/lock")
    @Operation(
            summary = "Manually lock user account",
            description = "Sets locked=true and lockedUntil=null for a manual lock. Admins cannot lock their own account.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "User locked successfully",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = ADMIN_ACCOUNT_STATE_EXAMPLE)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Self-lock rejected",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "message": "You cannot lock your own account",
                      "data": null,
                      "errors": []
                    }
                    """)))
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> lockUser(@PathVariable Long id) {
        User user = userService.lockUser(id, getCurrentUser());
        return ResponseEntity.ok(ApiResponse.success(
                "User locked successfully",
                userMapper.toAdminDetailResponse(user)));
    }

    @PutMapping("/{id}/unlock")
    @Operation(
            summary = "Unlock user account",
            description = "Sets locked=false, clears lockedUntil, and resets failedLoginAttempts to 0.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "User unlocked successfully",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = ADMIN_ACCOUNT_STATE_EXAMPLE)))
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> unlockUser(@PathVariable Long id) {
        User user = userService.unlockUser(id, getCurrentUser());
        return ResponseEntity.ok(ApiResponse.success(
                "User unlocked successfully",
                userMapper.toAdminDetailResponse(user)));
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new AuthenticationException("User not authenticated");
        }
        return (User) auth.getPrincipal();
    }

    private static final String ADMIN_ACCOUNT_STATE_EXAMPLE = """
            {
              "success": true,
              "message": "User status updated successfully",
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
            """;
}
