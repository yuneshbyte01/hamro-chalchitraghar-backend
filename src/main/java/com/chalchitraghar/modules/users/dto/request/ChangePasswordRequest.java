package com.chalchitraghar.modules.users.dto.request;

import com.chalchitraghar.shared.validation.StrongPassword;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    @Schema(example = "OldPass@123")
    private String currentPassword;

    @Size(min = 8, message = "New password must be at least 8 characters")
    @StrongPassword
    @NotBlank(message = "New password is required")
    @Schema(example = "NewStrongPass@123")
    private String newPassword;
}
