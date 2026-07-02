package com.chalchitraghar.modules.users.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Detailed response DTO for admin user lookup.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDetailResponse {

    private Long id;
    private String name;
    private String email;
    private String role;
    private String authProvider;
    private boolean emailVerified;
    private boolean enabled;
    private boolean locked;
    private int failedLoginAttempts;
    private LocalDateTime lockedUntil;
    private LocalDateTime lastLoginAt;
    private LocalDateTime passwordChangedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
