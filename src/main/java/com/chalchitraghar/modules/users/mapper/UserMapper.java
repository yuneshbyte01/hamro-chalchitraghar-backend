package com.chalchitraghar.modules.users.mapper;

import com.chalchitraghar.modules.users.dto.response.AdminUserDetailResponse;
import com.chalchitraghar.modules.users.dto.response.AdminUserSummaryResponse;
import com.chalchitraghar.modules.users.dto.response.UserResponse;
import com.chalchitraghar.modules.users.entity.User;
import org.springframework.stereotype.Component;

/** Mapper for converting User entities to safe response DTOs. */
@Component
public class UserMapper {

    /**
     * Converts a User entity to a customer profile response.
     *
     * @param user the entity to convert
     * @return the response DTO, or null if user is null
     */
    public UserResponse toResponseDto(User user) {
        if (user == null) {
            return null;
        }

        UserResponse dto = new UserResponse();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole() == null ? null : user.getRole().name());
        return dto;
    }

    /**
     * Converts a User entity to an admin list summary response.
     *
     * @param user the entity to convert
     * @return the response DTO, or null if user is null
     */
    public AdminUserSummaryResponse toAdminSummaryResponse(User user) {
        if (user == null) {
            return null;
        }

        AdminUserSummaryResponse dto = new AdminUserSummaryResponse();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole() == null ? null : user.getRole().name());
        dto.setEnabled(user.isEnabled());
        dto.setLocked(user.isLocked());
        return dto;
    }

    /**
     * Converts a User entity to an admin detail response.
     *
     * @param user the entity to convert
     * @return the response DTO, or null if user is null
     */
    public AdminUserDetailResponse toAdminDetailResponse(User user) {
        if (user == null) {
            return null;
        }

        AdminUserDetailResponse dto = new AdminUserDetailResponse();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole() == null ? null : user.getRole().name());
        dto.setAuthProvider(user.getAuthProvider() == null ? null : user.getAuthProvider().name());
        dto.setEmailVerified(user.isEmailVerified());
        dto.setEnabled(user.isEnabled());
        dto.setLocked(user.isLocked());
        dto.setFailedLoginAttempts(user.getFailedLoginAttempts());
        dto.setLockedUntil(user.getLockedUntil());
        dto.setLastLoginAt(user.getLastLoginAt());
        dto.setPasswordChangedAt(user.getPasswordChangedAt());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }
}
