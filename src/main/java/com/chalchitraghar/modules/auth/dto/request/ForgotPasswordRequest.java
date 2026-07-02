package com.chalchitraghar.modules.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {

    @Email(message = "Invalid email address")
    @NotBlank(message = "Email is required")
    @Schema(example = "user@example.com")
    private String email;
}
