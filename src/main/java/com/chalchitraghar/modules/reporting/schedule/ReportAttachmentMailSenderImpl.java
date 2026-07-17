package com.chalchitraghar.modules.reporting.schedule;

import com.chalchitraghar.modules.reporting.export.ReportExportResult;
import jakarta.mail.util.ByteArrayDataSource;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportAttachmentMailSenderImpl implements ReportAttachmentMailSender {
    private final JavaMailSender sender;

    @Value("${spring.mail.username:no-reply@hamrochalchitraghar.local}")
    private String from;

    public void send(String recipient, String subject, String body, ReportExportResult attachment) {
        try {
            MimeMessageHelper message =
                    new MimeMessageHelper(
                            sender.createMimeMessage(), true, StandardCharsets.UTF_8.name());
            message.setFrom(from);
            message.setTo(recipient);
            message.setSubject(subject.replaceAll("[\\r\\n]", " "));
            message.setText(body);
            message.addAttachment(
                    attachment.fileName(),
                    new ByteArrayDataSource(attachment.content(), attachment.contentType()));
            sender.send(message.getMimeMessage());
        } catch (Exception exception) {
            throw new IllegalStateException("Scheduled report email delivery failed", exception);
        }
    }
}
