package com.chalchitraghar.modules.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Request DTO for user registration.
 */
@Data
public class RegistrationRequest {

    @NotBlank(message = "Name is required")
    @Schema(example = "Aarav Sharma")
    private String name;

    @Email(message = "Invalid email address")
    @NotBlank(message = "Email is required")
    @Schema(example = "aarav@example.com")
    private String email;

    @Size(min = 8, message = "Password must be at least 8 characters")
    @NotBlank(message = "Password is required")
    @Schema(example = "StrongPass123")
    private String password;
}
