package com.chalchitraghar.modules.auth.dto.response;

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
