package com.chalchitraghar;

import static org.assertj.core.api.Assertions.assertThat;

import com.chalchitraghar.modules.notifications.enums.NotificationType;
import com.chalchitraghar.modules.notifications.service.*;
import com.chalchitraghar.modules.users.enums.Role;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(
        properties = {
            "app.notifications.retention.enabled=true",
            "app.notifications.retention.notification-days=1",
            "app.notifications.retention.delivery-days=1",
            "app.notifications.retention.batch-size=10"
        })
class NotificationRetentionIntegrationTest extends AbstractIntegrationTest {
    @Autowired private NotificationService notifications;
    @Autowired private NotificationEmailQueueService queue;
    @Autowired private NotificationRetentionProcessor retention;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void oldTerminalAuditIsAnonymizedWithoutDeletingEventKey() {
        var user = saveUser("retention@example.com", Role.CUSTOMER);
        var notification =
                notifications.createInAppNotification(
                        user.getId(),
                        NotificationType.WELCOME,
                        "USER_REGISTERED:" + user.getId(),
                        "Welcome",
                        "Sensitive historical content",
                        "{\"safe\":\"value\"}",
                        LocalDateTime.now(clock).minusDays(10));
        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now(clock).minusDays(9));
        notification.setUpdatedAt(LocalDateTime.now(clock).minusDays(9));
        notificationRepository.saveAndFlush(notification);
        jdbc.update(
                "update notifications set created_at = ? where id = ?",
                LocalDateTime.now(clock).minusDays(10),
                notification.getId());
        var delivery = queue.queue(notification);
        delivery.setUpdatedAt(LocalDateTime.now(clock).minusDays(9));
        notificationDeliveryRepository.saveAndFlush(delivery);
        jdbc.update(
                "update notification_deliveries set created_at = ? where id = ?",
                LocalDateTime.now(clock).minusDays(10),
                delivery.getId());

        assertThat(retention.processBatch()).isEqualTo(2);
        var archived = notificationRepository.findById(notification.getId()).orElseThrow();
        assertThat(archived.getAnonymizedAt()).isNotNull();
        assertThat(archived.getPayload()).isNull();
        assertThat(archived.getMessage())
                .isEqualTo("Notification content removed by retention policy.");
        assertThat(archived.getEventKey()).isEqualTo("USER_REGISTERED:" + user.getId());
        assertThat(
                        notificationDeliveryRepository
                                .findById(delivery.getId())
                                .orElseThrow()
                                .getRecipient())
                .isNull();
    }
}
