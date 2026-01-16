package com.chalchitraghar.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chalchitraghar.dto.auth.LoginRequestDto;
import com.chalchitraghar.dto.auth.LoginResponseDto;
import com.chalchitraghar.dto.auth.RegistrationRequestDto;
import com.chalchitraghar.dto.auth.RegistrationResponseDto;
import com.chalchitraghar.model.User;
import com.chalchitraghar.service.AuthService;
import com.chalchitraghar.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponseDto> register(@Valid @RequestBody RegistrationRequestDto requestDto) {
        User user = userService.registerUser(
                requestDto.getName(),
                requestDto.getEmail(),
                requestDto.getPassword()
        );

        RegistrationResponseDto response = new RegistrationResponseDto(
                "User registered successfully",
                user.getEmail()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        LoginResponseDto response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
