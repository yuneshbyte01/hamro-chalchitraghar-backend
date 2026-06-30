package com.chalchitraghar.modules.auth;

/**
 * Service for user authentication and registration operations.
 */
public interface AuthService {

    RegistrationResponse register(RegistrationRequest request);

    LoginResponse login(LoginRequest request);

    LoginResponse refreshToken(RefreshTokenRequest request);
}
