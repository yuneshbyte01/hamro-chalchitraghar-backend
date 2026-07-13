package com.chalchitraghar;

import static org.assertj.core.api.Assertions.assertThat;

import com.chalchitraghar.modules.notifications.entity.NotificationDelivery;
import com.chalchitraghar.modules.notifications.enums.*;
import com.chalchitraghar.modules.notifications.service.*;
import com.chalchitraghar.modules.users.enums.Role;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NotificationOperationsIntegrationTest extends AbstractIntegrationTest {
    @Autowired private NotificationService notifications;
    @Autowired private StaleNotificationDeliveryRecoveryService recovery;

    @Test
    void staleProcessingIsRecoveredWhileRecentClaimAndSentRemainUntouched() {
        var user = saveUser("stale@example.com", Role.CUSTOMER);
        var notification =
                notifications.createInAppNotification(
                        user.getId(),
                        NotificationType.WELCOME,
                        "USER_REGISTERED:" + user.getId(),
                        "Welcome",
                        "Welcome.",
                        null,
                        LocalDateTime.now(clock));
        var notification2 =
                notifications.createInAppNotification(
                        user.getId(),
                        NotificationType.SYSTEM,
                        "STALE:2",
                        "System",
                        "System.",
                        null,
                        LocalDateTime.now(clock));
        var notification3 =
                notifications.createInAppNotification(
                        user.getId(),
                        NotificationType.SYSTEM,
                        "STALE:3",
                        "System",
                        "System.",
                        null,
                        LocalDateTime.now(clock));
        NotificationDelivery stale =
                delivery(
                        notification,
                        NotificationDeliveryStatus.PROCESSING,
                        LocalDateTime.now(clock).minusMinutes(10));
        NotificationDelivery recent =
                delivery(
                        notification2,
                        NotificationDeliveryStatus.PROCESSING,
                        LocalDateTime.now(clock));
        NotificationDelivery sent =
                delivery(
                        notification3,
                        NotificationDeliveryStatus.SENT,
                        LocalDateTime.now(clock).minusMinutes(10));

        assertThat(recovery.recoverBatch()).isOne();
        assertThat(notificationDeliveryRepository.findById(stale.getId()).orElseThrow().getStatus())
                .isEqualTo(NotificationDeliveryStatus.FAILED);
        assertThat(
                        notificationDeliveryRepository
                                .findById(recent.getId())
                                .orElseThrow()
                                .getStatus())
                .isEqualTo(NotificationDeliveryStatus.PROCESSING);
        assertThat(notificationDeliveryRepository.findById(sent.getId()).orElseThrow().getStatus())
                .isEqualTo(NotificationDeliveryStatus.SENT);
    }

    private NotificationDelivery delivery(
            com.chalchitraghar.modules.notifications.entity.Notification n,
            NotificationDeliveryStatus status,
            LocalDateTime claimedAt) {
        LocalDateTime now = LocalDateTime.now(clock);
        NotificationDelivery d =
                NotificationDelivery.builder()
                        .notification(n)
                        .channel(NotificationChannel.EMAIL)
                        .recipient("stale@example.com")
                        .status(status)
                        .attemptCount(1)
                        .maxAttempts(3)
                        .claimedAt(claimedAt)
                        .claimedBy("worker")
                        .templateName("welcome")
                        .subject("Welcome")
                        .contentVersion(1)
                        .build();
        d.setCreatedAt(now);
        d.setUpdatedAt(now);
        return notificationDeliveryRepository.saveAndFlush(d);
    }
}
