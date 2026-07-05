package com.chalchitraghar.modules.users.dto.request;

import com.chalchitraghar.shared.validation.StrongPassword;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminCreateUserRequest {

    @NotBlank(message = "Name is required")
    @Schema(example = "John Doe")
    private String name;

    @Email(message = "Invalid email address")
    @NotBlank(message = "Email is required")
    @Schema(example = "john@example.com")
    private String email;

    @Size(min = 8, message = "Password must be at least 8 characters")
    @StrongPassword
    @NotBlank(message = "Password is required")
    @Schema(example = "StrongPass@123")
    private String password;

    @NotBlank(message = "Role is required")
    @Schema(example = "STAFF", allowableValues = {"CUSTOMER", "STAFF", "ADMIN"})
    private String role;
}
