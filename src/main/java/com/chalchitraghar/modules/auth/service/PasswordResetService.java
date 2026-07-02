package com.chalchitraghar.modules.auth.service;

public interface PasswordResetService {

    String FORGOT_PASSWORD_MESSAGE =
            "If an account exists with this email, password reset instructions have been sent.";

    void requestPasswordReset(String email);

    void resetPassword(String email, String otp, String newPassword);
}
