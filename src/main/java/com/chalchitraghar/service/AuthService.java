package com.chalchitraghar.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.chalchitraghar.dto.auth.LoginRequestDto;
import com.chalchitraghar.dto.auth.LoginResponseDto;
import com.chalchitraghar.exception.AuthenticationException;
import com.chalchitraghar.model.User;
import com.chalchitraghar.security.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginResponseDto login(LoginRequestDto request) {
        User user = userService.getUserByEmail(request.getEmail());

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthenticationException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user);

        return new LoginResponseDto(
            token,
            user.getEmail(),
            user.getName(),
            user.getRole().name()
        );
    }

}
