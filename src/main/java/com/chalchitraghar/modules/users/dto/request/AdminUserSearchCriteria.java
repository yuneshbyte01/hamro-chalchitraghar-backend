package com.chalchitraghar.modules.users.dto.request;

/** Search and filter criteria for admin user lookup. */
public record AdminUserSearchCriteria(
        String search,
        String role,
        String authProvider,
        Boolean enabled,
        Boolean locked,
        Boolean emailVerified) {}
