package com.chalchitraghar.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegistrationResponse {
    private String message; // Success or error message
    private String email; // User's email
}
