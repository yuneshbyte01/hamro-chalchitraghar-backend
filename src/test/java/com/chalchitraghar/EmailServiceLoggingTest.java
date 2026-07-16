package com.chalchitraghar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.chalchitraghar.modules.auth.service.impl.EmailServiceImpl;
import com.chalchitraghar.modules.users.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(OutputCaptureExtension.class)
class EmailServiceLoggingTest {

    @Test
    void disabledMailDoesNotLogOtpOrRecipient(CapturedOutput output) {
        EmailServiceImpl service = new EmailServiceImpl(mock(JavaMailSender.class));
        ReflectionTestUtils.setField(service, "mailEnabled", false);
        User user = User.builder().name("Customer").email("private@example.com").build();

        service.sendPasswordResetOtpEmail(user, "739201");

        assertThat(output.getOut())
                .contains("Password reset mail delivery skipped because mail is disabled")
                .doesNotContain("739201", "private@example.com");
    }
}
