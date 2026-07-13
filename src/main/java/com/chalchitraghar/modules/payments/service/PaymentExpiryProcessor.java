package com.chalchitraghar.modules.payments.service;

import com.chalchitraghar.modules.payments.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
public class PaymentExpiryProcessor {
    private final PaymentRepository repo;
    private final PaymentLifecycleService lifecycle;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void expire(Long id) {
        repo.findById(id)
                .ifPresent(
                        p -> {
                            if (lifecycle.reconcileExpiry(p)) repo.save(p);
                        });
    }
}
