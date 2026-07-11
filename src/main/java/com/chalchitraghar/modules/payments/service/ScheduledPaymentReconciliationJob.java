package com.chalchitraghar.modules.payments.service;
import java.time.*; import org.slf4j.*; import org.springframework.beans.factory.annotation.Value; import org.springframework.data.domain.PageRequest; import org.springframework.scheduling.annotation.Scheduled; import org.springframework.stereotype.Component;
import com.chalchitraghar.modules.payments.enums.*; import com.chalchitraghar.modules.payments.esewa.EsewaVerificationService; import com.chalchitraghar.modules.payments.repository.PaymentRepository; import lombok.RequiredArgsConstructor;
@Component @RequiredArgsConstructor
public class ScheduledPaymentReconciliationJob {
 private static final Logger log=LoggerFactory.getLogger(ScheduledPaymentReconciliationJob.class); private final PaymentRepository repo; private final EsewaVerificationService esewa; private final PaymentExpiryProcessor expiry; private final Clock clock;
 @Value("${app.payments.reconciliation-batch-size:100}") private int batch;
 @Scheduled(fixedDelayString="${app.payments.reconciliation-interval-ms:300000}") public void reconcile(){var due=repo.findByStatusOrderByCreatedAtAsc(PaymentStatus.PENDING,PageRequest.of(0,batch));for(var p:due){if(p.getProvider()!=PaymentProvider.ESEWA)continue;try{esewa.reconcile(p.getPaymentReference());}catch(RuntimeException e){log.warn("Payment reconciliation deferred for {}: provider unavailable or response invalid",p.getPaymentReference());}}}
 @Scheduled(fixedDelayString="${app.payments.expiry-interval-ms:60000}") public void expire(){var due=repo.findByStatusInAndExpiresAtBeforeOrderByExpiresAtAsc(PaymentLifecycleService.ACTIVE,LocalDateTime.now(clock),PageRequest.of(0,batch));due.forEach(p->expiry.expire(p.getId()));}
}
