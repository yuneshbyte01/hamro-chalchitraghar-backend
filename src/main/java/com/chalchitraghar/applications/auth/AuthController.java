package com.chalchitraghar.applications.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chalchitraghar.modules.auth.dto.request.LoginRequest;
import com.chalchitraghar.modules.auth.dto.response.LoginResponse;
import com.chalchitraghar.modules.auth.dto.request.RefreshTokenRequest;
import com.chalchitraghar.modules.auth.dto.request.RegistrationRequest;
import com.chalchitraghar.modules.auth.dto.response.RegistrationResponse;
import com.chalchitraghar.modules.auth.service.AuthService;
import com.chalchitraghar.shared.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for authentication endpoints.
 * All endpoints are public (no authentication required): login, register, token refresh.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Public authentication endpoints")
public class AuthController {

    private final AuthService authService;

    /**
     * Registers a new user account.
     *
     * @param request registration request containing user details
     * @return registration response with success message and email
     */
    @PostMapping("/register")
    @Operation(summary = "Register a customer account", description = "Creates a new user with CUSTOMER role and stores the password as a BCrypt hash.")
    public ResponseEntity<ApiResponse<RegistrationResponse>> register(@Valid @RequestBody RegistrationRequest request) {
        RegistrationResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response));
    }

    /**
     * Authenticates a user and returns a JWT token.
     *
     * @param request login request containing email and password
     * @return login response with JWT token and user details
     */
    @PostMapping("/login")
    @Operation(summary = "Login and get a JWT", description = "Authenticates credentials and returns a JWT with email and role claims.")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    /**
     * Refreshes a valid JWT token and returns a new token with user details.
     *
     * @param request refresh request containing the current token
     * @return login response with new JWT token and user details
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh a JWT", description = "Validates the current token and returns a new JWT.")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }
}
