package com.chalchitraghar.modules.shows.service;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.chalchitraghar.modules.shows.enums.ShowStatus;
import com.chalchitraghar.modules.shows.repository.ShowRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ShowStatusReconciliationJob {

    private final ShowRepository showRepository;
    private final ShowLifecycleService lifecycleService;

    @Scheduled(fixedDelayString = "${app.shows.status-reconciliation-interval-ms:60000}")
    @Transactional
    public void reconcileStatuses() {
        var changed = showRepository.findByStatusIn(List.of(ShowStatus.SCHEDULED, ShowStatus.RUNNING)).stream()
                .filter(lifecycleService::reconcile)
                .toList();
        if (!changed.isEmpty()) {
            showRepository.saveAll(changed);
        }
    }
}
