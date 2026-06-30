package com.chalchitraghar.modules.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Response DTO containing authentication token and user information.
 */
@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String email;
    private String name;
    private String role;
}
