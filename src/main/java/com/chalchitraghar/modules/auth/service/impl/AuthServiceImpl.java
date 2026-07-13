package com.chalchitraghar.modules.auth.service.impl;

import com.chalchitraghar.modules.auth.dto.GoogleUserInfo;
import com.chalchitraghar.modules.auth.dto.request.GoogleLoginRequest;
import com.chalchitraghar.modules.auth.dto.request.LoginRequest;
import com.chalchitraghar.modules.auth.dto.request.RefreshTokenRequest;
import com.chalchitraghar.modules.auth.dto.request.RegistrationRequest;
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
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    private static final int ACCOUNT_LOCK_MINUTES = 15;

    private final UserService userService;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final GoogleTokenVerifier googleTokenVerifier;

    @Override
    public RegistrationResponse register(RegistrationRequest request) {
        User user =
                userService.addUser(request.getName(), request.getEmail(), request.getPassword());
        return new RegistrationResponse("User registered successfully", user.getEmail());
    }

    @Override
    @Transactional(noRollbackFor = AuthenticationException.class)
    public LoginResponse login(LoginRequest request) {
        User user = userService.getUserByEmail(request.getEmail());
        ensureAccountCanAuthenticate(user);
        if (user.getAuthProvider() == AuthProvider.GOOGLE && user.getPassword() == null) {
            throw new AuthenticationException("This account uses Google Sign-In.");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            recordFailedLogin(user);
            throw new AuthenticationException("Invalid credentials");
        }
        recordSuccessfulLogin(user);
        return toLoginResponse(user);
    }

    @Override
    @Transactional
    public LoginResponse googleLogin(GoogleLoginRequest request) {
        GoogleUserInfo googleUser = googleTokenVerifier.verify(request.getIdToken());
        if (!googleUser.emailVerified()) {
            throw new AuthenticationException("Google email is not verified");
        }

        User user =
                userRepository
                        .findByEmail(googleUser.email())
                        .map(
                                existingUser -> {
                                    ensureAccountCanAuthenticate(existingUser);
                                    return linkOrUpdateGoogleAccount(existingUser, googleUser);
                                })
                        .orElseGet(() -> createGoogleUser(googleUser));

        ensureAccountCanAuthenticate(user);
        recordSuccessfulLogin(user);
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
        ensureAccountCanUseToken(user, token);
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
        User user =
                User.builder()
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

    private void ensureAccountCanAuthenticate(User user) {
        if (!user.isEnabled()) {
            throw new AuthenticationException("Account is disabled");
        }
        if (isLocked(user)) {
            throw new AuthenticationException(
                    "Account is temporarily locked. Please try again later.");
        }
        if (isLockExpired(user)) {
            clearLock(user);
            userRepository.save(user);
        }
    }

    private void ensureAccountCanUseToken(User user, String token) {
        ensureAccountCanAuthenticate(user);
        if (jwtUtil.wasIssuedBeforePasswordChanged(token, user)) {
            throw new AuthenticationException("Token is no longer valid after password change");
        }
    }

    private boolean isLocked(User user) {
        return user.isLocked()
                && (user.getLockedUntil() == null
                        || user.getLockedUntil().isAfter(LocalDateTime.now()));
    }

    private boolean isLockExpired(User user) {
        return user.isLocked()
                && user.getLockedUntil() != null
                && !user.getLockedUntil().isAfter(LocalDateTime.now());
    }

    private void recordFailedLogin(User user) {
        user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
        if (user.getFailedLoginAttempts() >= MAX_FAILED_LOGIN_ATTEMPTS) {
            user.setLocked(true);
            user.setLockedUntil(LocalDateTime.now().plusMinutes(ACCOUNT_LOCK_MINUTES));
        }
        userRepository.save(user);
    }

    private void recordSuccessfulLogin(User user) {
        clearLock(user);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
    }

    private void clearLock(User user) {
        user.setFailedLoginAttempts(0);
        user.setLocked(false);
        user.setLockedUntil(null);
    }

    private LoginResponse toLoginResponse(User user) {
        String token = jwtUtil.generateToken(user);
        return new LoginResponse(token, user.getEmail(), user.getName(), user.getRole().name());
    }
}
