package com.chalchitraghar.modules.notifications.service.impl;

import com.chalchitraghar.modules.notifications.service.*;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class NotificationEmailTemplateServiceImpl implements NotificationEmailTemplateService {
    private final TemplateEngine templates;

    @Override
    public RenderedNotificationEmail render(
            String templateName,
            String subject,
            String customerName,
            String title,
            String message) {
        Context model = new Context(Locale.ROOT);
        model.setVariable("customerName", customerName);
        model.setVariable("title", title);
        model.setVariable("message", message);
        String html = templates.process("email/" + templateName, model);
        return new RenderedNotificationEmail(subject, message, html);
    }
}
