package com.chalchitraghar.modules.auth.dto;

public record GoogleUserInfo(
        String googleId,
        String email,
        String name,
        String avatarUrl,
        boolean emailVerified
) {
}
