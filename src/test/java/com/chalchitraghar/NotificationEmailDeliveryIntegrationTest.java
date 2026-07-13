package com.chalchitraghar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.chalchitraghar.modules.notifications.entity.*;
import com.chalchitraghar.modules.notifications.enums.*;
import com.chalchitraghar.modules.notifications.service.*;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.modules.users.enums.Role;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSendException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@TestPropertySource(
        properties = {
            "app.notifications.email.enabled=true",
            "app.notifications.email.from=notifications@test.local",
            "app.notifications.email.async.enabled=false",
            "app.notifications.email.initial-retry-delay-ms=1000"
        })
class NotificationEmailDeliveryIntegrationTest extends AbstractIntegrationTest {
    @MockitoBean private NotificationMailSender mailSender;
    @Autowired private NotificationService notificationService;
    @Autowired private NotificationEmailQueueService queueService;
    @Autowired private NotificationEmailRetryProcessor retryProcessor;
    @Autowired private NotificationEmailTemplateService templateService;
    @Autowired private NotificationEmailDispatcher dispatcher;

    @Test
    void eligibleNotificationQueuesSnapshotsAndSendsOnce() {
        User user = saveUser("delivery-owner@example.com", Role.CUSTOMER);
        Notification notification =
                notificationService.createInAppNotification(
                        user.getId(),
                        NotificationType.WELCOME,
                        "USER_REGISTERED:" + user.getId(),
                        "Welcome",
                        "Welcome to Hamro Chalchitraghar.",
                        null,
                        LocalDateTime.now(clock));

        queueService.queue(notification);
        queueService.queue(notification);

        assertThat(notificationDeliveryRepository.findAll())
                .singleElement()
                .satisfies(
                        delivery -> {
                            assertThat(delivery.getRecipient())
                                    .isEqualTo("delivery-owner@example.com");
                            assertThat(delivery.getChannel()).isEqualTo(NotificationChannel.EMAIL);
                            assertThat(delivery.getStatus())
                                    .isEqualTo(NotificationDeliveryStatus.SENT);
                            assertThat(delivery.getAttemptCount()).isOne();
                            assertThat(delivery.getLastAttemptAt()).isNotNull();
                            assertThat(delivery.getSentAt()).isNotNull();
                            assertThat(delivery.getFailureReason()).isNull();
                        });
        verify(mailSender, times(1)).send(eq("delivery-owner@example.com"), any());
    }

    @Test
    void transientFailureSchedulesBoundedRetryAndSentDeliveryIsNotResent() {
        doThrow(new MailSendException("smtp unavailable"))
                .doNothing()
                .when(mailSender)
                .send(anyString(), any());
        User user = saveUser("delivery-retry@example.com", Role.CUSTOMER);
        Notification notification =
                notificationService.createInAppNotification(
                        user.getId(),
                        NotificationType.PAYMENT_SUCCEEDED,
                        "PAYMENT_SUCCEEDED:77",
                        "Payment successful",
                        "Payment for booking BK-77 completed successfully.",
                        null,
                        LocalDateTime.now(clock));

        queueService.queue(notification);
        NotificationDelivery failed = notificationDeliveryRepository.findAll().getFirst();
        assertThat(failed.getStatus()).isEqualTo(NotificationDeliveryStatus.FAILED);
        assertThat(failed.getAttemptCount()).isOne();
        assertThat(failed.getFailureReason()).isEqualTo("Temporary email delivery failure");
        assertThat(retryProcessor.processBatch()).isZero();

        failed.setNextAttemptAt(LocalDateTime.now(clock).minusSeconds(1));
        failed.setUpdatedAt(LocalDateTime.now(clock));
        notificationDeliveryRepository.saveAndFlush(failed);
        assertThat(retryProcessor.processBatch()).isOne();
        assertThat(
                        notificationDeliveryRepository
                                .findById(failed.getId())
                                .orElseThrow()
                                .getStatus())
                .isEqualTo(NotificationDeliveryStatus.SENT);
        assertThat(retryProcessor.processBatch()).isZero();
        verify(mailSender, times(2)).send(anyString(), any());
    }

    @Test
    void permanentFailureIsExhaustedAndStoresNoExceptionDetails() {
        doThrow(new IllegalStateException("secret smtp password and stack details"))
                .when(mailSender)
                .send(anyString(), any());
        User user = saveUser("delivery-permanent@example.com", Role.CUSTOMER);
        Notification notification =
                notificationService.createInAppNotification(
                        user.getId(),
                        NotificationType.BOOKING_CANCELLED,
                        "BOOKING_CANCELLED:88",
                        "Booking cancelled",
                        "Booking BK-88 was cancelled.",
                        null,
                        LocalDateTime.now(clock));

        queueService.queue(notification);

        NotificationDelivery delivery = notificationDeliveryRepository.findAll().getFirst();
        assertThat(delivery.getStatus()).isEqualTo(NotificationDeliveryStatus.EXHAUSTED);
        assertThat(delivery.getFailureReason())
                .isEqualTo("Email delivery failed permanently")
                .doesNotContain("password", "stack");
        assertThat(delivery.getNextAttemptAt()).isNull();
    }

    @Test
    void ticketIssuedIsExcludedAndTemplateEscapesUserControlledValues() {
        User user = saveUser("delivery-template@example.com", Role.CUSTOMER);
        Notification ticket =
                notificationService.createInAppNotification(
                        user.getId(),
                        NotificationType.TICKET_ISSUED,
                        "TICKET_ISSUED:91",
                        "Tickets ready",
                        "Tickets are ready.",
                        null,
                        LocalDateTime.now(clock));
        assertThat(queueService.queue(ticket)).isNull();

        var rendered =
                templateService.render(
                        "welcome",
                        "Welcome",
                        "<script>alert(1)</script>",
                        "Welcome",
                        "नमस्ते <b>customer</b>");
        assertThat(rendered.html())
                .contains("&lt;script&gt;alert(1)&lt;/script&gt;")
                .contains("नमस्ते &lt;b&gt;customer&lt;/b&gt;")
                .doesNotContain("<script>");
        assertThat(notificationDeliveryRepository.count()).isZero();
        verifyNoInteractions(mailSender);
    }

    @Test
    void concurrentDispatchersClaimAndSendPendingDeliveryOnlyOnce() throws Exception {
        User user = saveUser("delivery-concurrent@example.com", Role.CUSTOMER);
        LocalDateTime now = LocalDateTime.now(clock);
        Notification notification =
                notificationService.createInAppNotification(
                        user.getId(),
                        NotificationType.WELCOME,
                        "USER_REGISTERED:" + user.getId(),
                        "Welcome",
                        "Welcome.",
                        null,
                        now);
        NotificationDelivery delivery =
                NotificationDelivery.builder()
                        .notification(notification)
                        .channel(NotificationChannel.EMAIL)
                        .recipient(user.getEmail())
                        .status(NotificationDeliveryStatus.PENDING)
                        .attemptCount(0)
                        .maxAttempts(3)
                        .nextAttemptAt(now)
                        .templateName("welcome")
                        .subject("Welcome")
                        .contentVersion(1)
                        .build();
        delivery.setCreatedAt(now);
        delivery.setUpdatedAt(now);
        delivery = notificationDeliveryRepository.saveAndFlush(delivery);
        Long deliveryId = delivery.getId();

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> dispatcher.dispatch(deliveryId));
            var second = executor.submit(() -> dispatcher.dispatch(deliveryId));
            first.get();
            second.get();
        }

        NotificationDelivery sent =
                notificationDeliveryRepository.findById(deliveryId).orElseThrow();
        assertThat(sent.getStatus()).isEqualTo(NotificationDeliveryStatus.SENT);
        assertThat(sent.getAttemptCount()).isOne();
        verify(mailSender, times(1)).send(eq(user.getEmail()), any());
    }
}
