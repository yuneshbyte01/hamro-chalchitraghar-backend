package com.chalchitraghar.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.chalchitraghar.dto.auth.LoginRequest;
import com.chalchitraghar.dto.auth.LoginResponse;
import com.chalchitraghar.dto.auth.RegistrationRequest;
import com.chalchitraghar.dto.auth.RegistrationResponse;
import com.chalchitraghar.exception.AuthenticationException;
import com.chalchitraghar.model.User;
import com.chalchitraghar.security.JwtUtil;

import lombok.RequiredArgsConstructor;

/**
 * Service for user authentication and registration operations.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * Registers a new user and returns a registration response.
     *
     * @param request registration request containing user details
     * @return registration response with success message and email
     */
    public RegistrationResponse register(RegistrationRequest request) {
        User user = userService.addUser(
                request.getName(),
                request.getEmail(),
                request.getPassword()
        );

        return new RegistrationResponse(
                "User registered successfully",
                user.getEmail()
        );
    }

    /**
     * Authenticates a user and returns a login response with JWT token.
     *
     * @param request login request containing email and password
     * @return login response with JWT token and user details
     * @throws AuthenticationException if credentials are invalid
     */
    public LoginResponse login(LoginRequest request) {
        User user = userService.getUserByEmail(request.getEmail());

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthenticationException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user);

        return new LoginResponse(
            token,
            user.getEmail(),
            user.getName(),
            user.getRole().name()
        );
    }

}
