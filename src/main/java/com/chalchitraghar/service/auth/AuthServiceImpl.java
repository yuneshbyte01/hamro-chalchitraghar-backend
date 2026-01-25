package com.chalchitraghar.service.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.chalchitraghar.dto.auth.LoginRequest;
import com.chalchitraghar.dto.auth.LoginResponse;
import com.chalchitraghar.dto.auth.RefreshTokenRequest;
import com.chalchitraghar.dto.auth.RegistrationRequest;
import com.chalchitraghar.dto.auth.RegistrationResponse;
import com.chalchitraghar.exception.AuthenticationException;
import com.chalchitraghar.model.User;
import com.chalchitraghar.security.JwtUtil;
import com.chalchitraghar.service.admin.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
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

    @Override
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

    @Override
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        String token = request.getToken();
        if (!jwtUtil.validateToken(token)) {
            throw new AuthenticationException("Invalid or expired token");
        }
        String email = jwtUtil.extractUsername(token);
        User user = userService.getUserByEmail(email);
        String newToken = jwtUtil.generateToken(user);
        return new LoginResponse(
                newToken,
                user.getEmail(),
                user.getName(),
                user.getRole().name()
        );
    }
}
