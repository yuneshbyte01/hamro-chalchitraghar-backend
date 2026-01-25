package com.chalchitraghar.service.auth;

import com.chalchitraghar.dto.auth.LoginRequest;
import com.chalchitraghar.dto.auth.LoginResponse;
import com.chalchitraghar.dto.auth.RefreshTokenRequest;
import com.chalchitraghar.dto.auth.RegistrationRequest;
import com.chalchitraghar.dto.auth.RegistrationResponse;

/**
 * Service for user authentication and registration operations.
 */
public interface AuthService {

    RegistrationResponse register(RegistrationRequest request);

    LoginResponse login(LoginRequest request);

    LoginResponse refreshToken(RefreshTokenRequest request);
}
