package com.chalchitraghar.applications.auth;

import com.chalchitraghar.modules.auth.dto.request.ForgotPasswordRequest;
import com.chalchitraghar.modules.auth.dto.request.GoogleLoginRequest;
import com.chalchitraghar.modules.auth.dto.request.LoginRequest;
import com.chalchitraghar.modules.auth.dto.request.RefreshTokenRequest;
import com.chalchitraghar.modules.auth.dto.request.RegistrationRequest;
import com.chalchitraghar.modules.auth.dto.request.ResetPasswordRequest;
import com.chalchitraghar.modules.auth.dto.response.LoginResponse;
import com.chalchitraghar.modules.auth.dto.response.RegistrationResponse;
import com.chalchitraghar.modules.auth.service.AuthService;
import com.chalchitraghar.modules.auth.service.PasswordResetService;
import com.chalchitraghar.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication endpoints. All endpoints are public (no authentication
 * required): login, register, token refresh.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Public authentication endpoints")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    /**
     * Registers a new user account.
     *
     * @param request registration request containing user details
     * @return registration response with success message and email
     */
    @PostMapping("/register")
    @Operation(
            summary = "Register a customer account",
            description =
                    "Creates a new user with CUSTOMER role and stores the password as a BCrypt hash.")
    public ResponseEntity<ApiResponse<RegistrationResponse>> register(
            @Valid @RequestBody RegistrationRequest request) {
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
    @Operation(
            summary = "Login and get a JWT",
            description =
                    "Authenticates local credentials and returns a JWT with email and role claims. Disabled accounts, locked accounts, and Google-only accounts are rejected.")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/google")
    @Operation(
            summary = "Login with Google",
            description =
                    "Verifies a Google ID token, links or creates the account, and returns a JWT.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Google login successful",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                            {
                              "success": true,
                              "message": "Login successful",
                              "data": {
                                "token": "eyJhbGciOiJIUzI1NiJ9...",
                                "email": "aarav@example.com",
                                "name": "Aarav Sharma",
                                "role": "CUSTOMER"
                              },
                              "errors": []
                            }
                            """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Missing Google ID token",
                        content = @Content(mediaType = "application/json")),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "401",
                        description =
                                "Invalid token, expired token, audience mismatch, unverified Google email, disabled account, or locked account",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                            {
                              "success": false,
                              "message": "Invalid Google ID token",
                              "data": null,
                              "errors": []
                            }
                            """)))
            })
    public ResponseEntity<ApiResponse<LoginResponse>> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request) {
        LoginResponse response = authService.googleLogin(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    /**
     * Refreshes a valid JWT token and returns a new token with user details.
     *
     * @param request refresh request containing the current token
     * @return login response with new JWT token and user details
     */
    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh a JWT",
            description =
                    "Validates the current token and returns a new JWT. Tokens issued before the user's latest password change are rejected.")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @PostMapping("/forgot-password")
    @Operation(
            summary = "Request a password reset OTP",
            description =
                    "Always returns a generic success message. If the email belongs to an account, a reset OTP is sent.")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(PasswordResetService.FORGOT_PASSWORD_MESSAGE));
    }

    @PostMapping("/reset-password")
    @Operation(
            summary = "Reset password with email OTP",
            description =
                    "Validates the latest one-time email OTP, stores the new password as a BCrypt hash, and marks the OTP used.")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(
                request.getEmail(), request.getOtp(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully"));
    }
}
