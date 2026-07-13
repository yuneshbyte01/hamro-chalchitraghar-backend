package com.chalchitraghar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

import com.chalchitraghar.modules.notifications.enums.*;
import com.chalchitraghar.modules.notifications.service.*;
import com.chalchitraghar.modules.users.enums.Role;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class NotificationEmailDisabledIntegrationTest extends AbstractIntegrationTest {
    @MockitoBean private NotificationMailSender mailSender;
    @Autowired private NotificationService notificationService;
    @Autowired private NotificationEmailQueueService queueService;

    @Test
    void disabledDeliveryCreatesAuditableSkippedRowWithoutSending() {
        var user = saveUser("delivery-disabled@example.com", Role.CUSTOMER);
        var notification =
                notificationService.createInAppNotification(
                        user.getId(),
                        NotificationType.WELCOME,
                        "USER_REGISTERED:" + user.getId(),
                        "Welcome",
                        "Welcome.",
                        null,
                        LocalDateTime.now(clock));

        queueService.queue(notification);

        assertThat(notificationRepository.count()).isOne();
        assertThat(notificationDeliveryRepository.findAll())
                .singleElement()
                .satisfies(
                        delivery -> {
                            assertThat(delivery.getStatus())
                                    .isEqualTo(NotificationDeliveryStatus.SKIPPED);
                            assertThat(delivery.getFailureReason())
                                    .isEqualTo("Email delivery disabled");
                            assertThat(delivery.getAttemptCount()).isZero();
                        });
        verifyNoInteractions(mailSender);
    }
}
