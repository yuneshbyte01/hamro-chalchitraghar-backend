package com.chalchitraghar.modules.notifications.service.impl;

import com.chalchitraghar.modules.notifications.config.NotificationEmailProperties;
import com.chalchitraghar.modules.notifications.entity.*;
import com.chalchitraghar.modules.notifications.enums.*;
import com.chalchitraghar.modules.notifications.repository.*;
import com.chalchitraghar.modules.notifications.service.*;
import jakarta.mail.internet.InternetAddress;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class NotificationEmailQueueServiceImpl implements NotificationEmailQueueService {
    private final NotificationDeliveryRepository deliveries;
    private final NotificationRepository notifications;
    private final NotificationEmailPolicy policy;
    private final NotificationEmailProperties properties;
    private final NotificationEmailDispatchLauncher launcher;
    private final Clock clock;
    private final PlatformTransactionManager transactionManager;
    private final NotificationPreferenceService preferenceService;

    @Override
    public NotificationDelivery queue(Notification supplied) {
        if (!policy.eligible(supplied.getType())) return null;
        try {
            return requiresNew().execute(status -> createOrReuse(supplied.getId()));
        } catch (DataIntegrityViolationException race) {
            return requiresNew()
                    .execute(
                            status ->
                                    deliveries
                                            .findByNotificationIdAndChannel(
                                                    supplied.getId(), NotificationChannel.EMAIL)
                                            .orElseThrow(() -> race));
        }
    }

    private NotificationDelivery createOrReuse(Long notificationId) {
        Notification notification = notifications.findById(notificationId).orElseThrow();
        var existing =
                deliveries.findByNotificationIdAndChannel(
                        notification.getId(), NotificationChannel.EMAIL);
        if (existing.isPresent()) return existing.get();

        LocalDateTime now = LocalDateTime.now(clock);
        String recipient = normalize(notification.getUser().getEmail());
        NotificationDeliveryStatus status = NotificationDeliveryStatus.PENDING;
        String reason = null;
        if (!properties.isEnabled()) {
            status = NotificationDeliveryStatus.SKIPPED;
            reason = "Email delivery disabled";
        } else if (!preferenceService.emailEnabled(
                notification.getUser().getId(), notification.getType())) {
            status = NotificationDeliveryStatus.SKIPPED;
            reason = "Disabled by user preference";
        } else if (!valid(recipient)) {
            status = NotificationDeliveryStatus.SKIPPED;
            reason = "Invalid email recipient";
        }
        NotificationDelivery delivery =
                NotificationDelivery.builder()
                        .notification(notification)
                        .channel(NotificationChannel.EMAIL)
                        .recipient(recipient)
                        .status(status)
                        .attemptCount(0)
                        .maxAttempts(properties.getMaxAttempts())
                        .nextAttemptAt(status == NotificationDeliveryStatus.PENDING ? now : null)
                        .failureReason(reason)
                        .templateName(policy.template(notification.getType()))
                        .subject(notification.getTitle())
                        .contentVersion(1)
                        .build();
        delivery.setCreatedAt(now);
        delivery.setUpdatedAt(now);
        NotificationDelivery saved = deliveries.saveAndFlush(delivery);
        if (saved.getStatus() == NotificationDeliveryStatus.PENDING) afterCommit(saved.getId());
        return saved;
    }

    private void afterCommit(Long deliveryId) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        launcher.submit(deliveryId);
                    }
                });
    }

    private String normalize(String email) {
        return email == null ? null : email.trim();
    }

    private boolean valid(String email) {
        if (email == null || email.isBlank() || email.length() > 320) return false;
        try {
            new InternetAddress(email, true).validate();
            return true;
        } catch (Exception invalid) {
            return false;
        }
    }

    private TransactionTemplate requiresNew() {
        var transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transaction;
    }
}
