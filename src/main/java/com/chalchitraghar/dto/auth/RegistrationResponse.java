package com.chalchitraghar.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Response DTO for user registration operation.
 */
@Data
@AllArgsConstructor
public class RegistrationResponse {
    private String message;
    private String email;
}
