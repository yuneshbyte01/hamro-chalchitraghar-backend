package com.chalchitraghar.modules.notifications.service.impl;

import com.chalchitraghar.modules.notifications.config.NotificationEmailProperties;
import com.chalchitraghar.modules.notifications.entity.NotificationDelivery;
import com.chalchitraghar.modules.notifications.enums.NotificationDeliveryStatus;
import com.chalchitraghar.modules.notifications.repository.NotificationDeliveryRepository;
import com.chalchitraghar.modules.notifications.service.*;
import com.chalchitraghar.shared.observability.BusinessMetric;
import com.chalchitraghar.shared.observability.BusinessMetrics;
import com.chalchitraghar.shared.observability.BusinessOperation;
import com.chalchitraghar.shared.observability.MetricOutcome;
import java.net.*;
import java.time.*;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEmailDispatcherImpl implements NotificationEmailDispatcher {
    private final NotificationDeliveryRepository deliveries;
    private final NotificationEmailTemplateService templates;
    private final NotificationMailSender mailSender;
    private final NotificationEmailProperties properties;
    private final PlatformTransactionManager transactionManager;
    private final Clock clock;
    private final BusinessMetrics metrics;
    private final String workerId = "notification-" + UUID.randomUUID().toString().substring(0, 8);

    @Override
    public void dispatch(Long deliveryId) {
        Claim claim = requiresNew().execute(status -> claim(deliveryId));
        if (claim == null) return;
        try {
            RenderedNotificationEmail rendered =
                    templates.render(
                            claim.templateName(),
                            claim.subject(),
                            claim.customerName(),
                            claim.title(),
                            claim.message());
            mailSender.send(claim.recipient(), rendered);
            requiresNew().executeWithoutResult(status -> complete(claim.id()));
            metrics.increment(BusinessMetric.EMAIL, BusinessOperation.SEND, MetricOutcome.SUCCESS);
        } catch (RuntimeException failure) {
            requiresNew().executeWithoutResult(status -> fail(claim.id(), failure));
            metrics.increment(BusinessMetric.EMAIL, BusinessOperation.SEND, MetricOutcome.FAILURE);
        }
    }

    private Claim claim(Long id) {
        NotificationDelivery delivery = deliveries.findByIdForUpdate(id).orElse(null);
        if (delivery == null || !claimable(delivery, LocalDateTime.now(clock))) return null;
        LocalDateTime now = LocalDateTime.now(clock);
        delivery.setStatus(NotificationDeliveryStatus.PROCESSING);
        delivery.setClaimedAt(now);
        delivery.setClaimedBy(workerId);
        delivery.setLastAttemptAt(now);
        delivery.setAttemptCount(delivery.getAttemptCount() + 1);
        delivery.setUpdatedAt(now);
        var notification = delivery.getNotification();
        deliveries.save(delivery);
        return new Claim(
                delivery.getId(),
                delivery.getRecipient(),
                delivery.getTemplateName(),
                delivery.getSubject(),
                notification.getUser().getName(),
                notification.getTitle(),
                notification.getMessage());
    }

    private boolean claimable(NotificationDelivery delivery, LocalDateTime now) {
        if (delivery.getAttemptCount() >= delivery.getMaxAttempts()) return false;
        if (delivery.getStatus() == NotificationDeliveryStatus.PENDING
                || delivery.getStatus() == NotificationDeliveryStatus.FAILED)
            return delivery.getNextAttemptAt() == null || !delivery.getNextAttemptAt().isAfter(now);
        return delivery.getStatus() == NotificationDeliveryStatus.PROCESSING
                && delivery.getClaimedAt() != null
                && !delivery.getClaimedAt()
                        .plus(Duration.ofMillis(properties.getProcessingTimeoutMs()))
                        .isAfter(now);
    }

    private void complete(Long id) {
        NotificationDelivery delivery = deliveries.findByIdForUpdate(id).orElseThrow();
        if (delivery.getStatus() != NotificationDeliveryStatus.PROCESSING) return;
        LocalDateTime now = LocalDateTime.now(clock);
        delivery.setStatus(NotificationDeliveryStatus.SENT);
        delivery.setSentAt(now);
        delivery.setFailedAt(null);
        delivery.setFailureReason(null);
        delivery.setNextAttemptAt(null);
        clearClaim(delivery);
        delivery.setUpdatedAt(now);
        deliveries.save(delivery);
    }

    private void fail(Long id, RuntimeException failure) {
        NotificationDelivery delivery = deliveries.findByIdForUpdate(id).orElseThrow();
        if (delivery.getStatus() != NotificationDeliveryStatus.PROCESSING) return;
        LocalDateTime now = LocalDateTime.now(clock);
        boolean retryable = retryable(failure);
        boolean exhausted = delivery.getAttemptCount() >= delivery.getMaxAttempts();
        delivery.setStatus(
                !retryable || exhausted
                        ? NotificationDeliveryStatus.EXHAUSTED
                        : NotificationDeliveryStatus.FAILED);
        delivery.setFailedAt(now);
        delivery.setFailureReason(
                retryable
                        ? "Temporary email delivery failure"
                        : "Email delivery failed permanently");
        delivery.setNextAttemptAt(
                delivery.getStatus() == NotificationDeliveryStatus.FAILED
                        ? now.plus(Duration.ofMillis(backoff(delivery.getAttemptCount())))
                        : null);
        clearClaim(delivery);
        delivery.setUpdatedAt(now);
        deliveries.save(delivery);
        log.warn(
                "Notification email failed deliveryId={} attempt={} status={} reason={}",
                id,
                delivery.getAttemptCount(),
                delivery.getStatus(),
                failure.getClass().getSimpleName());
    }

    private boolean retryable(RuntimeException failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof MailException
                    || current instanceof SocketException
                    || current instanceof SocketTimeoutException
                    || current instanceof ConnectException) return true;
            current = current.getCause();
        }
        return false;
    }

    private long backoff(int attempt) {
        double calculated =
                properties.getInitialRetryDelayMs()
                        * Math.pow(properties.getRetryMultiplier(), Math.max(0, attempt - 1));
        return Math.min(properties.getMaxRetryDelayMs(), (long) calculated);
    }

    private void clearClaim(NotificationDelivery delivery) {
        delivery.setClaimedAt(null);
        delivery.setClaimedBy(null);
    }

    private TransactionTemplate requiresNew() {
        var transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transaction;
    }

    private record Claim(
            Long id,
            String recipient,
            String templateName,
            String subject,
            String customerName,
            String title,
            String message) {}
}
