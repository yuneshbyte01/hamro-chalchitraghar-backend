package com.chalchitraghar.modules.notifications.service.impl;

import com.chalchitraghar.modules.notifications.config.NotificationEmailProperties;
import com.chalchitraghar.modules.notifications.service.*;
import jakarta.mail.MessagingException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationMailSenderImpl implements NotificationMailSender {
    private final JavaMailSender sender;
    private final NotificationEmailProperties properties;

    @Override
    public void send(String recipient, RenderedNotificationEmail email) {
        try {
            MimeMessageHelper message =
                    new MimeMessageHelper(
                            sender.createMimeMessage(), false, StandardCharsets.UTF_8.name());
            message.setFrom(properties.getFrom(), properties.getFromName());
            message.setTo(recipient);
            message.setSubject(email.subject());
            message.setText(email.plainText(), email.html());
            sender.send(message.getMimeMessage());
        } catch (MessagingException | java.io.UnsupportedEncodingException exception) {
            throw new IllegalStateException("Unable to construct notification email", exception);
        }
    }
}
