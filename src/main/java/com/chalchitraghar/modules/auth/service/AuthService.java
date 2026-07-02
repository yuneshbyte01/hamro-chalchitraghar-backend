package com.chalchitraghar.modules.auth.service;

import com.chalchitraghar.modules.auth.dto.request.LoginRequest;
import com.chalchitraghar.modules.auth.dto.request.GoogleLoginRequest;
import com.chalchitraghar.modules.auth.dto.request.RefreshTokenRequest;
import com.chalchitraghar.modules.auth.dto.request.RegistrationRequest;
import com.chalchitraghar.modules.auth.dto.response.LoginResponse;
import com.chalchitraghar.modules.auth.dto.response.RegistrationResponse;
/**
 * Service for user authentication and registration operations.
 */
public interface AuthService {

    RegistrationResponse register(RegistrationRequest request);

    LoginResponse login(LoginRequest request);

    LoginResponse googleLogin(GoogleLoginRequest request);

    LoginResponse refreshToken(RefreshTokenRequest request);
}
