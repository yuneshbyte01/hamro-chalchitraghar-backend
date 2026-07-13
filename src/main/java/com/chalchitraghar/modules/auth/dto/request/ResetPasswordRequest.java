package com.chalchitraghar.modules.auth.dto.request;

import com.chalchitraghar.shared.validation.StrongPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @Email(message = "Invalid email address")
    @NotBlank(message = "Email is required")
    @Schema(example = "user@example.com")
    private String email;

    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "\\d{6}", message = "OTP must be a 6-digit number")
    @Schema(example = "123456")
    private String otp;

    @Size(min = 8, message = "New password must be at least 8 characters")
    @StrongPassword
    @NotBlank(message = "New password is required")
    @Schema(example = "NewStrongPass@123")
    private String newPassword;
}
