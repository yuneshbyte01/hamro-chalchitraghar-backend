package com.chalchitraghar.modules.shows.service;

import com.chalchitraghar.modules.shows.enums.ShowStatus;
import com.chalchitraghar.modules.shows.repository.ShowRepository;
import com.chalchitraghar.shared.observability.JobName;
import com.chalchitraghar.shared.observability.ScheduledJobObserver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ShowStatusReconciliationJob {

    private final ShowRepository showRepository;
    private final ShowLifecycleService lifecycleService;
    private final ScheduledJobObserver jobs;

    @Scheduled(fixedDelayString = "${app.shows.status-reconciliation-interval-ms:60000}")
    @Transactional
    public void reconcileStatuses() {
        jobs.observe(JobName.SHOW_RECONCILIATION, this::reconcileObserved);
    }

    private int reconcileObserved() {
        var changed =
                showRepository
                        .findByStatusIn(List.of(ShowStatus.SCHEDULED, ShowStatus.RUNNING))
                        .stream()
                        .filter(lifecycleService::reconcile)
                        .toList();
        if (!changed.isEmpty()) {
            showRepository.saveAll(changed);
        }
        return changed.size();
    }
}
