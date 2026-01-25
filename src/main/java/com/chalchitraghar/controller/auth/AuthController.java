package com.chalchitraghar.controller.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chalchitraghar.dto.auth.LoginRequest;
import com.chalchitraghar.dto.auth.LoginResponse;
import com.chalchitraghar.dto.auth.RefreshTokenRequest;
import com.chalchitraghar.dto.auth.RegistrationRequest;
import com.chalchitraghar.dto.auth.RegistrationResponse;
import com.chalchitraghar.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for authentication endpoints.
 * All endpoints are public (no authentication required): login, register, token refresh.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Registers a new user account.
     *
     * @param request registration request containing user details
     * @return registration response with success message and email
     */
    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegistrationRequest request) {
        RegistrationResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticates a user and returns a JWT token.
     *
     * @param request login request containing email and password
     * @return login response with JWT token and user details
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Refreshes a valid JWT token and returns a new token with user details.
     *
     * @param request refresh request containing the current token
     * @return login response with new JWT token and user details
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }
}
