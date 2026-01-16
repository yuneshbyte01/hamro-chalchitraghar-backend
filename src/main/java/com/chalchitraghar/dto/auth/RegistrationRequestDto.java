package com.chalchitraghar.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegistrationRequestDto {

    @NotBlank(message = "Name is required")
    private String name; // User's name

    @Email(message = "Invalid email address")
    @NotBlank(message = "Email is required")
    private String email; // User's email

    @Size(min = 8, message = "Password must be at least 8 characters")
    @NotBlank(message = "Password is required")
    private String password; // User's password
}
