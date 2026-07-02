package com.chalchitraghar.modules.auth.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.chalchitraghar.modules.auth.service.EmailService;
import com.chalchitraghar.modules.users.entity.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final Environment environment;

    @Value("${app.mail.from}")
    private String mailFrom;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.password-reset.otp-expiration-minutes}")
    private long otpExpirationMinutes;

    @Override
    public void sendPasswordResetOtpEmail(User user, String otp) {
        if (!mailEnabled) {
            if (isDevelopmentProfile()) {
                logger.info("Password reset mail disabled. OTP for {}: {}", user.getEmail(), otp);
            } else {
                logger.info("Password reset mail disabled for {}", user.getEmail());
            }
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(user.getEmail());
        message.setSubject("Hamro Chalchitraghar Password Reset OTP");
        message.setText("""
                Hello %s,

                We received a request to reset your password.

                Your password reset verification code is:
                %s

                This OTP is valid for %d minutes.

                If you did not request this password reset, please ignore this email.

                Hamro Chalchitraghar Team
                """.formatted(user.getName(), otp, otpExpirationMinutes));

        mailSender.send(message);
    }

    private boolean isDevelopmentProfile() {
        for (String profile : environment.getActiveProfiles()) {
            if ("dev".equals(profile)) {
                return true;
            }
        }
        return false;
    }
}
