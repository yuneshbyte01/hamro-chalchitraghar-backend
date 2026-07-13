package com.chalchitraghar.modules.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Request DTO for user authentication. */
@Data
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    @Schema(example = "aarav@example.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Schema(example = "StrongPass123")
    private String password;
}
