package com.chalchitraghar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.chalchitraghar.modules.auth.dto.GoogleUserInfo;
import com.chalchitraghar.modules.auth.entity.PasswordResetOtp;
import com.chalchitraghar.modules.auth.service.EmailService;
import com.chalchitraghar.modules.auth.service.GoogleTokenVerifier;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.modules.users.enums.AuthProvider;
import com.chalchitraghar.modules.users.enums.Role;
import com.chalchitraghar.shared.exception.AuthenticationException;
import com.chalchitraghar.shared.security.JwtUtil;

class AuthApiIntegrationTest extends AbstractIntegrationTest {

    private static final String STRONG_PASSWORD = "Password123!";
    private static final String FORGOT_PASSWORD_MESSAGE =
            "If an account exists with this email, password reset instructions have been sent.";

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private GoogleTokenVerifier googleTokenVerifier;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void registerCustomerSuccessfullyWithStrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "name", "Sita",
                                "email", "sita@example.com",
                                "password", STRONG_PASSWORD))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.email").value("sita@example.com"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void registerFailsWithoutUppercase() throws Exception {
        expectWeakPasswordRegistrationFails("password123!");
    }

    @Test
    void registerFailsWithoutLowercase() throws Exception {
        expectWeakPasswordRegistrationFails("PASSWORD123!");
    }

    @Test
    void registerFailsWithoutDigit() throws Exception {
        expectWeakPasswordRegistrationFails("Password!");
    }

    @Test
    void registerFailsWithoutSpecialCharacter() throws Exception {
        expectWeakPasswordRegistrationFails("Password123");
    }

    @Test
    void loginSuccessfully() throws Exception {
        saveUser("login@example.com", Role.CUSTOMER);

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(json(Map.of("email", "login@example.com", "password", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void duplicateEmailRegistrationReturnsError() throws Exception {
        saveUser("duplicate@example.com", Role.CUSTOMER);

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "name", "Duplicate",
                                "email", "duplicate@example.com",
                                "password", STRONG_PASSWORD))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email is already registered"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void invalidLoginReturnsUnauthorizedErrorResponse() throws Exception {
        saveUser("wrong-password@example.com", Role.CUSTOMER);

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(json(Map.of("email", "wrong-password@example.com", "password", "wrongpass123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid credentials"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void passwordIsStoredHashed() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "name", "Hashed User",
                                "email", "hashed@example.com",
                                "password", STRONG_PASSWORD))))
                .andExpect(status().isCreated());

        User user = userRepository.findByEmail("hashed@example.com").orElseThrow();
        assertThat(user.getPassword()).isNotEqualTo(STRONG_PASSWORD);
        assertThat(passwordEncoder.matches(STRONG_PASSWORD, user.getPassword())).isTrue();
    }

    @Test
    void refreshTokenSuccessfully() throws Exception {
        String token = tokenFor("refresh@example.com", Role.CUSTOMER);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType("application/json")
                        .content(json(Map.of("token", token))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.email").value("refresh@example.com"))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void invalidRefreshTokenReturnsUnauthorizedErrorResponse() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType("application/json")
                        .content(json(Map.of("token", "invalid-token"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid or expired token"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void googleLoginWithValidTokenCreatesNewGoogleAccount() throws Exception {
        when(googleTokenVerifier.verify("valid-google-token"))
                .thenReturn(googleUser("google-123", "google-new@example.com", true));

        String token = googleLoginToken("valid-google-token", "google-new@example.com");

        User user = userRepository.findByEmail("google-new@example.com").orElseThrow();
        assertThat(user.getAuthProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(user.getPassword()).isNull();
        assertThat(user.getGoogleId()).isEqualTo("google-123");
        assertThat(user.getAvatarUrl()).isEqualTo("https://example.com/avatar.png");
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("google-new@example.com");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("CUSTOMER");
    }

    @Test
    void googleLoginWithInvalidTokenFails() throws Exception {
        when(googleTokenVerifier.verify("invalid-google-token"))
                .thenThrow(new AuthenticationException("Invalid Google ID token"));

        mockMvc.perform(post("/api/auth/google")
                        .contentType("application/json")
                        .content(json(Map.of("idToken", "invalid-google-token"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid Google ID token"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void googleLoginWithExpiredTokenFails() throws Exception {
        when(googleTokenVerifier.verify("expired-google-token"))
                .thenThrow(new AuthenticationException("Invalid Google ID token"));

        mockMvc.perform(post("/api/auth/google")
                        .contentType("application/json")
                        .content(json(Map.of("idToken", "expired-google-token"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid Google ID token"));
    }

    @Test
    void googleLoginWithUnverifiedEmailFails() throws Exception {
        when(googleTokenVerifier.verify("unverified-google-token"))
                .thenReturn(googleUser("google-unverified", "unverified@example.com", false));

        mockMvc.perform(post("/api/auth/google")
                        .contentType("application/json")
                        .content(json(Map.of("idToken", "unverified-google-token"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Google email is not verified"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void googleLoginWithExistingGoogleAccountLogsInAndUpdatesAvatar() throws Exception {
        saveGoogleUser("existing-google@example.com", "google-existing", "https://example.com/old.png");
        when(googleTokenVerifier.verify("existing-google-token"))
                .thenReturn(new GoogleUserInfo(
                        "google-existing",
                        "existing-google@example.com",
                        "Google Existing",
                        "https://example.com/new.png",
                        true));

        googleLoginToken("existing-google-token", "existing-google@example.com");

        User user = userRepository.findByEmail("existing-google@example.com").orElseThrow();
        assertThat(user.getAuthProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(user.getGoogleId()).isEqualTo("google-existing");
        assertThat(user.getAvatarUrl()).isEqualTo("https://example.com/new.png");
        assertThat(user.getPassword()).isNull();
    }

    @Test
    void googleLoginLinksExistingLocalAccountWithoutOverwritingPassword() throws Exception {
        saveCustomerWithPassword("local-link@example.com", "OldPass@123");
        when(googleTokenVerifier.verify("link-google-token"))
                .thenReturn(googleUser("google-linked", "local-link@example.com", true));

        googleLoginToken("link-google-token", "local-link@example.com");

        User user = userRepository.findByEmail("local-link@example.com").orElseThrow();
        assertThat(user.getAuthProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(user.getGoogleId()).isEqualTo("google-linked");
        assertThat(user.getAvatarUrl()).isEqualTo("https://example.com/avatar.png");
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(passwordEncoder.matches("OldPass@123", user.getPassword())).isTrue();

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "email", "local-link@example.com",
                                "password", "OldPass@123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    void localLoginStillWorksForLocalAccount() throws Exception {
        saveCustomerWithPassword("local-still-works@example.com", "OldPass@123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "email", "local-still-works@example.com",
                                "password", "OldPass@123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    void passwordLoginForGoogleAccountFailsGracefully() throws Exception {
        saveGoogleUser("google-password@example.com", "google-password", "https://example.com/avatar.png");

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "email", "google-password@example.com",
                                "password", "AnyPass@123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("This account uses Google Sign-In."))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray());

        verify(googleTokenVerifier, org.mockito.Mockito.never()).verify(anyString());
    }

    @Test
    void failedLoginIncrementsAttempts() throws Exception {
        saveCustomerWithPassword("attempts@example.com", "OldPass@123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "email", "attempts@example.com",
                                "password", "WrongPass@123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));

        User user = userRepository.findByEmail("attempts@example.com").orElseThrow();
        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
        assertThat(user.isLocked()).isFalse();
    }

    @Test
    void accountLocksAfterFiveFailedAttempts() throws Exception {
        saveCustomerWithPassword("lock-after-failures@example.com", "OldPass@123");

        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType("application/json")
                            .content(json(Map.of(
                                    "email", "lock-after-failures@example.com",
                                    "password", "WrongPass@123"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid credentials"));
        }

        User user = userRepository.findByEmail("lock-after-failures@example.com").orElseThrow();
        assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(user.isLocked()).isTrue();
        assertThat(user.getLockedUntil()).isAfter(LocalDateTime.now());
    }

    @Test
    void lockedAccountCannotLogin() throws Exception {
        User user = saveCustomerWithPassword("locked-login@example.com", "OldPass@123");
        user.setLocked(true);
        user.setLockedUntil(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "email", "locked-login@example.com",
                                "password", "OldPass@123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Account is temporarily locked. Please try again later."));
    }

    @Test
    void accountLoginWorksAfterLockExpiry() throws Exception {
        User user = saveCustomerWithPassword("expired-lock@example.com", "OldPass@123");
        user.setFailedLoginAttempts(5);
        user.setLocked(true);
        user.setLockedUntil(LocalDateTime.now().minusMinutes(1));
        userRepository.save(user);

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "email", "expired-lock@example.com",
                                "password", "OldPass@123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty());

        User updated = userRepository.findByEmail("expired-lock@example.com").orElseThrow();
        assertThat(updated.isLocked()).isFalse();
        assertThat(updated.getLockedUntil()).isNull();
        assertThat(updated.getFailedLoginAttempts()).isZero();
    }

    @Test
    void successfulLoginResetsAttemptsAndUpdatesLastLoginAt() throws Exception {
        User user = saveCustomerWithPassword("successful-reset@example.com", "OldPass@123");
        user.setFailedLoginAttempts(3);
        userRepository.save(user);

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "email", "successful-reset@example.com",
                                "password", "OldPass@123"))))
                .andExpect(status().isOk());

        User updated = userRepository.findByEmail("successful-reset@example.com").orElseThrow();
        assertThat(updated.getFailedLoginAttempts()).isZero();
        assertThat(updated.getLastLoginAt()).isNotNull();
    }

    @Test
    void registrationSetsPasswordChangedAt() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "name", "Tracked Password",
                                "email", "tracked-register@example.com",
                                "password", STRONG_PASSWORD))))
                .andExpect(status().isCreated());

        User user = userRepository.findByEmail("tracked-register@example.com").orElseThrow();
        assertThat(user.getPasswordChangedAt()).isNotNull();
    }

    @Test
    void passwordChangeUpdatesPasswordChangedAt() throws Exception {
        User user = saveCustomerWithPassword("tracked-change@example.com", "OldPass@123");
        user.setPasswordChangedAt(LocalDateTime.now().minusDays(1));
        userRepository.save(user);
        String token = loginTokenWithPassword("tracked-change@example.com", "OldPass@123");

        mockMvc.perform(put("/api/customer/profile/password")
                        .header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content(json(Map.of(
                                "currentPassword", "OldPass@123",
                                "newPassword", "NewStrongPass@123"))))
                .andExpect(status().isOk());

        User updated = userRepository.findByEmail("tracked-change@example.com").orElseThrow();
        assertThat(updated.getPasswordChangedAt()).isAfter(user.getPasswordChangedAt());
    }

    @Test
    void otpResetUpdatesPasswordChangedAt() throws Exception {
        User user = saveCustomerWithPassword("tracked-reset@example.com", "OldPass@123");
        user.setPasswordChangedAt(LocalDateTime.now().minusDays(1));
        userRepository.save(user);
        String otp = requestPasswordResetOtp("tracked-reset@example.com");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "email", "tracked-reset@example.com",
                                "otp", otp,
                                "newPassword", "NewStrongPass@123"))))
                .andExpect(status().isOk());

        User updated = userRepository.findByEmail("tracked-reset@example.com").orElseThrow();
        assertThat(updated.getPasswordChangedAt()).isAfter(user.getPasswordChangedAt());
    }

    @Test
    void disabledAccountCannotLogin() throws Exception {
        User user = saveCustomerWithPassword("disabled-login@example.com", "OldPass@123");
        user.setEnabled(false);
        userRepository.save(user);

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "email", "disabled-login@example.com",
                                "password", "OldPass@123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Account is disabled"));
    }

    @Test
    void disabledAccountTokenCannotAccessProtectedEndpoint() throws Exception {
        saveCustomerWithPassword("disabled-token@example.com", "OldPass@123");
        String token = loginTokenWithPassword("disabled-token@example.com", "OldPass@123");
        User user = userRepository.findByEmail("disabled-token@example.com").orElseThrow();
        user.setEnabled(false);
        userRepository.save(user);

        mockMvc.perform(get("/api/customer/profile")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Account is disabled"));
    }

    @Test
    void googleLoginUpdatesLastLoginAt() throws Exception {
        saveGoogleUser("google-last-login@example.com", "google-last-login", "https://example.com/old.png");
        when(googleTokenVerifier.verify("google-last-login-token"))
                .thenReturn(googleUser("google-last-login", "google-last-login@example.com", true));

        googleLoginToken("google-last-login-token", "google-last-login@example.com");

        User user = userRepository.findByEmail("google-last-login@example.com").orElseThrow();
        assertThat(user.getLastLoginAt()).isNotNull();
    }

    @Test
    void lockedGoogleAccountCannotLogin() throws Exception {
        User user = saveGoogleUser("locked-google@example.com", "locked-google", "https://example.com/avatar.png");
        user.setLocked(true);
        user.setLockedUntil(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);
        when(googleTokenVerifier.verify("locked-google-token"))
                .thenReturn(googleUser("locked-google", "locked-google@example.com", true));

        mockMvc.perform(post("/api/auth/google")
                        .contentType("application/json")
                        .content(json(Map.of("idToken", "locked-google-token"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Account is temporarily locked. Please try again later."));
    }

    @Test
    void disabledGoogleAccountCannotLogin() throws Exception {
        User user = saveGoogleUser("disabled-google@example.com", "disabled-google", "https://example.com/avatar.png");
        user.setEnabled(false);
        userRepository.save(user);
        when(googleTokenVerifier.verify("disabled-google-token"))
                .thenReturn(googleUser("disabled-google", "disabled-google@example.com", true));

        mockMvc.perform(post("/api/auth/google")
                        .contentType("application/json")
                        .content(json(Map.of("idToken", "disabled-google-token"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Account is disabled"));
    }

    @Test
    void oldJwtCannotAccessProtectedEndpointAfterPasswordChange() throws Exception {
        saveCustomerWithPassword("old-token@example.com", "OldPass@123");
        String oldToken = loginTokenWithPassword("old-token@example.com", "OldPass@123");

        mockMvc.perform(put("/api/customer/profile/password")
                        .header("Authorization", bearer(oldToken))
                        .contentType("application/json")
                        .content(json(Map.of(
                                "currentPassword", "OldPass@123",
                                "newPassword", "NewStrongPass@123"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/customer/profile")
                        .header("Authorization", bearer(oldToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Token is no longer valid after password change"));
    }

    @Test
    void forgotPasswordWithExistingEmailReturnsGenericSuccess() throws Exception {
        saveCustomerWithPassword("forgot-existing@example.com", "OldPass@123");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType("application/json")
                        .content(json(Map.of("email", "forgot-existing@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(FORGOT_PASSWORD_MESSAGE))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray());

        verify(emailService).sendPasswordResetOtpEmail(any(User.class), any(String.class));
        assertThat(passwordResetOtpRepository.findAll()).hasSize(1);
    }

    @Test
    void forgotPasswordWithUnknownEmailReturnsSameGenericSuccess() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType("application/json")
                        .content(json(Map.of("email", "missing@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(FORGOT_PASSWORD_MESSAGE))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray());

        verifyNoInteractions(emailService);
        assertThat(passwordResetOtpRepository.findAll()).isEmpty();
    }

    @Test
    void forgotPasswordStoresHashedOtpOnly() throws Exception {
        saveCustomerWithPassword("hashed-otp@example.com", "OldPass@123");

        String otp = requestPasswordResetOtp("hashed-otp@example.com");

        PasswordResetOtp resetOtp = passwordResetOtpRepository.findAll().getFirst();
        assertThat(resetOtp.getOtpHash()).isNotEqualTo(otp);
        assertThat(resetOtp.getOtpHash()).isNotBlank();
        assertThat(resetOtp.getAttemptCount()).isZero();
        assertThat(resetOtp.getUsedAt()).isNull();
    }

    @Test
    void resetPasswordSuccessfullyChangesPassword() throws Exception {
        saveCustomerWithPassword("reset-success@example.com", "OldPass@123");
        String otp = requestPasswordResetOtp("reset-success@example.com");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "email", "reset-success@example.com",
                                "otp", otp,
                                "newPassword", "NewStrongPass@123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password reset successfully"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray());

        PasswordResetOtp resetOtp = passwordResetOtpRepository.findAll().getFirst();
        assertThat(resetOtp.getUsedAt()).isNotNull();

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "email", "reset-success@example.com",
                                "password", "OldPass@123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "email", "reset-success@example.com",
                                "password", "NewStrongPass@123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    void resetPasswordWithInvalidOtpFails() throws Exception {
        saveCustomerWithPassword("invalid-otp@example.com", "OldPass@123");
        String otp = requestPasswordResetOtp("invalid-otp@example.com");
        String invalidOtp = differentOtpThan(otp);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "email", "invalid-otp@example.com",
                                "otp", invalidOtp,
                                "newPassword", "NewStrongPass@123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid or expired password reset OTP"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray());

        assertThat(passwordResetOtpRepository.findAll().getFirst().getAttemptCount()).isEqualTo(1);
    }

    @Test
    void resetPasswordWithExpiredOtpFails() throws Exception {
        saveCustomerWithPassword("expired-reset@example.com", "OldPass@123");
        String otp = requestPasswordResetOtp("expired-reset@example.com");
        PasswordResetOtp resetOtp = passwordResetOtpRepository.findAll().getFirst();
        resetOtp.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        passwordResetOtpRepository.save(resetOtp);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "email", "expired-reset@example.com",
                                "otp", otp,
                                "newPassword", "NewStrongPass@123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid or expired password reset OTP"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void resetPasswordWithReusedOtpFails() throws Exception {
        saveCustomerWithPassword("reused-reset@example.com", "OldPass@123");
        String otp = requestPasswordResetOtp("reused-reset@example.com");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "email", "reused-reset@example.com",
                                "otp", otp,
                                "newPassword", "NewStrongPass@123"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "email", "reused-reset@example.com",
                                "otp", otp,
                                "newPassword", "AnotherStrongPass@123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid or expired password reset OTP"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void resetPasswordFailsAfterExceededOtpAttemptLimit() throws Exception {
        saveCustomerWithPassword("max-attempts@example.com", "OldPass@123");
        String otp = requestPasswordResetOtp("max-attempts@example.com");
        String invalidOtp = differentOtpThan(otp);

        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/auth/reset-password")
                            .contentType("application/json")
                            .content(json(Map.of(
                                    "email", "max-attempts@example.com",
                                    "otp", invalidOtp,
                                    "newPassword", "NewStrongPass@123"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid or expired password reset OTP"));
        }

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "email", "max-attempts@example.com",
                                "otp", invalidOtp,
                                "newPassword", "NewStrongPass@123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Maximum password reset OTP attempts exceeded"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray());

        assertThat(passwordResetOtpRepository.findAll().getFirst().getAttemptCount()).isEqualTo(5);
    }

    @Test
    void resetPasswordWithWeakPasswordFails() throws Exception {
        saveCustomerWithPassword("weak-reset@example.com", "OldPass@123");
        String otp = requestPasswordResetOtp("weak-reset@example.com");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "email", "weak-reset@example.com",
                                "otp", otp,
                                "newPassword", "weakpassword"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0]", containsString("newPassword:")));
    }

    @Test
    void resetPasswordWithSameOldPasswordFails() throws Exception {
        saveCustomerWithPassword("same-reset@example.com", "OldPass@123");
        String otp = requestPasswordResetOtp("same-reset@example.com");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "email", "same-reset@example.com",
                                "otp", otp,
                                "newPassword", "OldPass@123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("New password must be different from current password"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void customerProfileReturnsCurrentUserForValidCustomerToken() throws Exception {
        String token = tokenFor("profile@example.com", Role.CUSTOMER);

        mockMvc.perform(get("/api/customer/profile")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Profile fetched successfully"))
                .andExpect(jsonPath("$.data.name").value("CUSTOMER User"))
                .andExpect(jsonPath("$.data.email").value("profile@example.com"))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void customerProfileUpdateChangesName() throws Exception {
        String token = tokenFor("profile-update@example.com", Role.CUSTOMER);

        mockMvc.perform(put("/api/customer/profile")
                        .header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content(json(Map.of("name", "Updated Name"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Profile updated successfully"))
                .andExpect(jsonPath("$.data.name").value("Updated Name"))
                .andExpect(jsonPath("$.data.email").value("profile-update@example.com"))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.errors").isArray());

        User user = userRepository.findByEmail("profile-update@example.com").orElseThrow();
        assertThat(user.getName()).isEqualTo("Updated Name");
    }

    @Test
    void customerProfileUpdateRejectsBlankName() throws Exception {
        String token = tokenFor("blank-name@example.com", Role.CUSTOMER);

        mockMvc.perform(put("/api/customer/profile")
                        .header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content(json(Map.of("name", " "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0]", containsString("name:")));
    }

    @Test
    void customerProfileUpdateDoesNotAllowEmailOrRoleUpdate() throws Exception {
        String token = tokenFor("protected-profile@example.com", Role.CUSTOMER);

        mockMvc.perform(put("/api/customer/profile")
                        .header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content(json(Map.of(
                                "name", "Protected Name",
                                "email", "changed@example.com",
                                "role", "ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Protected Name"))
                .andExpect(jsonPath("$.data.email").value("protected-profile@example.com"))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"));

        User user = userRepository.findByEmail("protected-profile@example.com").orElseThrow();
        assertThat(user.getName()).isEqualTo("Protected Name");
        assertThat(user.getEmail()).isEqualTo("protected-profile@example.com");
        assertThat(user.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(userRepository.findByEmail("changed@example.com")).isEmpty();
    }

    @Test
    void customerPasswordChangeSuccessfullyChangesPassword() throws Exception {
        saveCustomerWithPassword("change-password@example.com", "OldPass@123");
        String token = loginTokenWithPassword("change-password@example.com", "OldPass@123");

        mockMvc.perform(put("/api/customer/profile/password")
                        .header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content(json(Map.of(
                                "currentPassword", "OldPass@123",
                                "newPassword", "NewStrongPass@123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password changed successfully"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray());

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "email", "change-password@example.com",
                                "password", "OldPass@123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "email", "change-password@example.com",
                                "password", "NewStrongPass@123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    void customerPasswordChangeFailsWithWrongCurrentPassword() throws Exception {
        saveCustomerWithPassword("wrong-current@example.com", "OldPass@123");
        String token = loginTokenWithPassword("wrong-current@example.com", "OldPass@123");

        mockMvc.perform(put("/api/customer/profile/password")
                        .header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content(json(Map.of(
                                "currentPassword", "WrongPass@123",
                                "newPassword", "NewStrongPass@123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Current password is incorrect"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void customerPasswordChangeFailsWithWeakNewPassword() throws Exception {
        saveCustomerWithPassword("weak-new@example.com", "OldPass@123");
        String token = loginTokenWithPassword("weak-new@example.com", "OldPass@123");

        mockMvc.perform(put("/api/customer/profile/password")
                        .header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content(json(Map.of(
                                "currentPassword", "OldPass@123",
                                "newPassword", "weakpassword"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0]", containsString("newPassword:")));
    }

    @Test
    void customerPasswordChangeFailsWhenNewPasswordMatchesCurrentPassword() throws Exception {
        saveCustomerWithPassword("same-password@example.com", "SamePass@123");
        String token = loginTokenWithPassword("same-password@example.com", "SamePass@123");

        mockMvc.perform(put("/api/customer/profile/password")
                        .header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content(json(Map.of(
                                "currentPassword", "SamePass@123",
                                "newPassword", "SamePass@123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("New password must be different from current password"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void unauthenticatedProfileUpdateReturnsUnauthorized() throws Exception {
        mockMvc.perform(put("/api/customer/profile")
                        .contentType("application/json")
                        .content(json(Map.of("name", "Updated Name"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void unauthenticatedPasswordChangeReturnsUnauthorized() throws Exception {
        mockMvc.perform(put("/api/customer/profile/password")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "currentPassword", "OldPass@123",
                                "newPassword", "NewStrongPass@123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray());
    }

    private void expectWeakPasswordRegistrationFails(String password) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "name", "Weak Password",
                                "email", "weak-" + password.hashCode() + "@example.com",
                                "password", password))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0]", containsString("password:")));
    }

    private User saveCustomerWithPassword(String email, String password) {
        User user = User.builder()
                .name("CUSTOMER User")
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(Role.CUSTOMER)
                .authProvider(AuthProvider.LOCAL)
                .emailVerified(false)
                .build();
        return userRepository.save(user);
    }

    private User saveGoogleUser(String email, String googleId, String avatarUrl) {
        User user = User.builder()
                .name("Google User")
                .email(email)
                .password(null)
                .role(Role.CUSTOMER)
                .authProvider(AuthProvider.GOOGLE)
                .googleId(googleId)
                .avatarUrl(avatarUrl)
                .emailVerified(true)
                .build();
        return userRepository.save(user);
    }

    private String loginTokenWithPassword(String email, String password) throws Exception {
        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(json(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("token")
                .asText();
    }

    private String requestPasswordResetOtp(String email) throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType("application/json")
                        .content(json(Map.of("email", email))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(FORGOT_PASSWORD_MESSAGE));

        ArgumentCaptor<String> otp = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetOtpEmail(any(User.class), otp.capture());
        return otp.getValue();
    }

    private String differentOtpThan(String otp) {
        return "000000".equals(otp) ? "000001" : "000000";
    }

    private GoogleUserInfo googleUser(String googleId, String email, boolean emailVerified) {
        return new GoogleUserInfo(
                googleId,
                email,
                "Google User",
                "https://example.com/avatar.png",
                emailVerified);
    }

    private String googleLoginToken(String idToken, String expectedEmail) throws Exception {
        var result = mockMvc.perform(post("/api/auth/google")
                        .contentType("application/json")
                        .content(json(Map.of("idToken", idToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.email").value(expectedEmail))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.errors").isArray())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("token")
                .asText();
    }
}
