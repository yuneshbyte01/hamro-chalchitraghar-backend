package com.chalchitraghar;

import static org.assertj.core.api.Assertions.assertThat;

import com.chalchitraghar.modules.notifications.enums.NotificationType;
import com.chalchitraghar.modules.notifications.event.BookingCreatedEvent;
import com.chalchitraghar.modules.notifications.event.UserRegisteredEvent;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.modules.users.enums.Role;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class NotificationEventIntegrationTest extends AbstractIntegrationTest {
    @Autowired private ApplicationEventPublisher events;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    void committedTypedEventCreatesIdempotentInAppNotificationWithEventTime() {
        User user = saveUser("notification-event@example.com", Role.CUSTOMER);
        LocalDateTime occurredAt =
                LocalDateTime.now(clock)
                        .minusMinutes(3)
                        .truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        BookingCreatedEvent event =
                new BookingCreatedEvent(user.getId(), 42L, "BK-PUBLIC-42", "Jatra", occurredAt);

        inTransaction(() -> events.publishEvent(event));
        inTransaction(() -> events.publishEvent(event));

        assertThat(notificationRepository.findAll())
                .singleElement()
                .satisfies(
                        notification -> {
                            assertThat(notification.getType())
                                    .isEqualTo(NotificationType.BOOKING_CREATED);
                            assertThat(notification.getOccurredAt()).isEqualTo(occurredAt);
                            assertThat(notification.getTitle()).isEqualTo("Booking initiated");
                            assertThat(notification.getMessage())
                                    .contains("BK-PUBLIC-42")
                                    .doesNotContain("confirmed");
                            assertThat(notification.getEventKey()).isEqualTo("BOOKING_CREATED:42");
                        });
    }

    @Test
    void rolledBackTransactionDoesNotCreateNotification() {
        User user = saveUser("notification-rollback@example.com", Role.CUSTOMER);
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(
                        status -> {
                            events.publishEvent(
                                    new UserRegisteredEvent(
                                            user.getId(), LocalDateTime.now(clock)));
                            status.setRollbackOnly();
                        });

        assertThat(notificationRepository.count()).isZero();
    }

    @Test
    void sameEventKeyForDifferentRecipientsCreatesSeparateNotifications() {
        User first = saveUser("notification-event-first@example.com", Role.CUSTOMER);
        User second = saveUser("notification-event-second@example.com", Role.CUSTOMER);
        LocalDateTime occurredAt = LocalDateTime.now(clock);

        inTransaction(
                () -> events.publishEvent(new UserRegisteredEvent(first.getId(), occurredAt)));
        inTransaction(
                () -> events.publishEvent(new UserRegisteredEvent(second.getId(), occurredAt)));

        assertThat(notificationRepository.count()).isEqualTo(2);
    }

    private void inTransaction(Runnable action) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> action.run());
    }
}
