package com.chalchitraghar.modules.auth.service;

import com.chalchitraghar.modules.users.entity.User;

public interface EmailService {

    void sendPasswordResetOtpEmail(User user, String otp);
}
