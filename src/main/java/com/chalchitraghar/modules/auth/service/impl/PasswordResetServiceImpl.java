package com.chalchitraghar.modules.auth.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chalchitraghar.modules.auth.entity.PasswordResetOtp;
import com.chalchitraghar.modules.auth.repository.PasswordResetOtpRepository;
import com.chalchitraghar.modules.auth.service.EmailService;
import com.chalchitraghar.modules.auth.service.PasswordResetService;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.modules.users.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final int OTP_BOUND = 1_000_000;
    private static final String INVALID_OTP_MESSAGE = "Invalid or expired password reset OTP";

    private final UserRepository userRepository;
    private final PasswordResetOtpRepository passwordResetOtpRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.password-reset.otp-expiration-minutes}")
    private long otpExpirationMinutes;

    @Value("${app.password-reset.max-attempts}")
    private int maxAttempts;

    @Override
    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (!isPasswordResetRequestAllowed(user)) {
                return;
            }

            invalidatePreviousOtps(user);

            String otp = generateOtp();
            PasswordResetOtp resetOtp = PasswordResetOtp.builder()
                    .user(user)
                    .otpHash(hashOtp(otp))
                    .expiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes))
                    .attemptCount(0)
                    .build();
            passwordResetOtpRepository.save(resetOtp);

            emailService.sendPasswordResetOtpEmail(user, otp);
        });
    }

    @Override
    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public void resetPassword(String email, String otp, String newPassword) {
        PasswordResetOtp resetOtp = passwordResetOtpRepository
                .findFirstByUserEmailAndUsedAtIsNullOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new IllegalArgumentException(INVALID_OTP_MESSAGE));

        if (resetOtp.getUsedAt() != null) {
            throw new IllegalArgumentException("Password reset OTP has already been used");
        }
        if (resetOtp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(INVALID_OTP_MESSAGE);
        }
        if (resetOtp.getAttemptCount() >= maxAttempts) {
            throw new IllegalArgumentException("Maximum password reset OTP attempts exceeded");
        }
        if (!hashOtp(otp).equals(resetOtp.getOtpHash())) {
            resetOtp.setAttemptCount(resetOtp.getAttemptCount() + 1);
            passwordResetOtpRepository.save(resetOtp);
            throw new IllegalArgumentException(INVALID_OTP_MESSAGE);
        }

        User user = resetOtp.getUser();
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("New password must be different from current password");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setFailedLoginAttempts(0);
        user.setLocked(false);
        user.setLockedUntil(null);
        resetOtp.setUsedAt(LocalDateTime.now());
        passwordResetOtpRepository.save(resetOtp);
        userRepository.save(user);
    }

    public String hashOtp(String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(otp.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 hashing is not available", e);
        }
    }

    private String generateOtp() {
        return "%06d".formatted(secureRandom.nextInt(OTP_BOUND));
    }

    private void invalidatePreviousOtps(User user) {
        LocalDateTime now = LocalDateTime.now();
        passwordResetOtpRepository.findByUserIdAndUsedAtIsNull(user.getId())
                .forEach(otp -> otp.setUsedAt(now));
    }

    private boolean isPasswordResetRequestAllowed(User user) {
        return true;
    }
}
