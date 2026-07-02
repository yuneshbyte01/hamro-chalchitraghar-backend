package com.chalchitraghar.modules.auth.service.impl;

import com.chalchitraghar.modules.auth.dto.request.LoginRequest;
import com.chalchitraghar.modules.auth.dto.request.GoogleLoginRequest;
import com.chalchitraghar.modules.auth.dto.request.RefreshTokenRequest;
import com.chalchitraghar.modules.auth.dto.request.RegistrationRequest;
import com.chalchitraghar.modules.auth.dto.GoogleUserInfo;
import com.chalchitraghar.modules.auth.dto.response.LoginResponse;
import com.chalchitraghar.modules.auth.dto.response.RegistrationResponse;
import com.chalchitraghar.modules.auth.service.AuthService;
import com.chalchitraghar.modules.auth.service.GoogleTokenVerifier;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.modules.users.enums.AuthProvider;
import com.chalchitraghar.modules.users.enums.Role;
import com.chalchitraghar.modules.users.repository.UserRepository;
import com.chalchitraghar.modules.users.service.UserService;
import com.chalchitraghar.shared.exception.AuthenticationException;
import com.chalchitraghar.shared.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final GoogleTokenVerifier googleTokenVerifier;

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
        if (user.getAuthProvider() == AuthProvider.GOOGLE && user.getPassword() == null) {
            throw new AuthenticationException("This account uses Google Sign-In.");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthenticationException("Invalid credentials");
        }
        return toLoginResponse(user);
    }

    @Override
    public LoginResponse googleLogin(GoogleLoginRequest request) {
        GoogleUserInfo googleUser = googleTokenVerifier.verify(request.getIdToken());
        if (!googleUser.emailVerified()) {
            throw new AuthenticationException("Google email is not verified");
        }

        User user = userRepository.findByEmail(googleUser.email())
                .map(existingUser -> linkOrUpdateGoogleAccount(existingUser, googleUser))
                .orElseGet(() -> createGoogleUser(googleUser));

        return toLoginResponse(user);
    }

    @Override
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        String token = request.getToken();
        if (!jwtUtil.validateToken(token)) {
            throw new AuthenticationException("Invalid or expired token");
        }
        String email = jwtUtil.extractUsername(token);
        User user = userService.getUserByEmail(email);
        return toLoginResponse(user);
    }

    private User linkOrUpdateGoogleAccount(User user, GoogleUserInfo googleUser) {
        user.setGoogleId(googleUser.googleId());
        user.setAvatarUrl(googleUser.avatarUrl());
        user.setEmailVerified(true);
        if (user.getAuthProvider() == null) {
            user.setAuthProvider(AuthProvider.LOCAL);
        }
        return userRepository.save(user);
    }

    private User createGoogleUser(GoogleUserInfo googleUser) {
        User user = User.builder()
                .name(googleUser.name())
                .email(googleUser.email())
                .password(null)
                .role(Role.CUSTOMER)
                .authProvider(AuthProvider.GOOGLE)
                .googleId(googleUser.googleId())
                .avatarUrl(googleUser.avatarUrl())
                .emailVerified(true)
                .build();
        return userRepository.save(user);
    }

    private LoginResponse toLoginResponse(User user) {
        String token = jwtUtil.generateToken(user);
        return new LoginResponse(
                token,
                user.getEmail(),
                user.getName(),
                user.getRole().name()
        );
    }
}
