package com.chalchitraghar.modules.notifications.listener;

import com.chalchitraghar.modules.notifications.event.*;
import com.chalchitraghar.modules.notifications.service.NotificationContentFactory;
import com.chalchitraghar.modules.notifications.service.NotificationEmailQueueService;
import com.chalchitraghar.modules.notifications.service.NotificationService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class BusinessNotificationEventListener {
    private final NotificationContentFactory contentFactory;
    private final NotificationService notifications;
    private final NotificationEmailQueueService emailQueue;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(UserRegisteredEvent event) {
        handle(event, event.userId(), event.occurredAt());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(BookingCreatedEvent event) {
        handle(event, event.userId(), event.occurredAt());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(BookingConfirmedEvent event) {
        handle(event, event.userId(), event.occurredAt());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(BookingCancelledEvent event) {
        handle(event, event.userId(), event.occurredAt());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(BookingExpiredEvent event) {
        handle(event, event.userId(), event.occurredAt());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(PaymentSucceededEvent event) {
        handle(event, event.userId(), event.occurredAt());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(PaymentFailedEvent event) {
        handle(event, event.userId(), event.occurredAt());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ShowUpdatedEvent event) {
        handle(event, event.userId(), event.occurredAt());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ShowCancelledEvent event) {
        handle(event, event.userId(), event.occurredAt());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ShowReminderDueEvent event) {
        handle(event, event.userId(), event.occurredAt());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(TicketIssuedNotificationEvent event) {
        handle(event, event.userId(), event.occurredAt());
    }

    private void handle(Object event, Long userId, LocalDateTime occurredAt) {
        var content = contentFactory.build(event);
        try {
            var notification =
                    notifications.createInAppNotification(
                            userId,
                            content.type(),
                            content.eventKey(),
                            content.title(),
                            content.message(),
                            content.payload(),
                            occurredAt);
            try {
                emailQueue.queue(notification);
            } catch (RuntimeException queueFailure) {
                log.error(
                        "Notification email queueing failed eventType={} eventKey={} recipientUserId={} reason={}",
                        event.getClass().getSimpleName(),
                        content.eventKey(),
                        userId,
                        queueFailure.getClass().getSimpleName());
            }
            log.info(
                    "Created notification eventType={} eventKey={} recipientUserId={} notificationType={}",
                    event.getClass().getSimpleName(),
                    content.eventKey(),
                    userId,
                    content.type());
        } catch (RuntimeException exception) {
            log.error(
                    "Notification creation failed eventType={} eventKey={} recipientUserId={} notificationType={} reason={}",
                    event.getClass().getSimpleName(),
                    content.eventKey(),
                    userId,
                    content.type(),
                    exception.getClass().getSimpleName());
        }
    }
}
