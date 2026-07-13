package com.chalchitraghar;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chalchitraghar.modules.audit.service.AuditBusinessPublisher;
import com.chalchitraghar.modules.shows.entity.Show;
import com.chalchitraghar.modules.shows.enums.ShowStatus;
import com.chalchitraghar.modules.shows.repository.ShowRepository;
import com.chalchitraghar.modules.shows.service.ShowLifecycleService;
import com.chalchitraghar.modules.shows.service.ShowStatusReconciliationJob;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShowStatusReconciliationJobTest {

    @Test
    void jobPersistsStaleActiveStatuses() {
        ShowRepository repository = mock(ShowRepository.class);
        Clock clock =
                Clock.fixed(Instant.parse("2026-07-11T06:30:00Z"), ZoneId.of("Asia/Kathmandu"));
        Show stale =
                Show.builder()
                        .showDate(LocalDate.of(2026, 7, 11))
                        .showTime(LocalTime.of(10, 0))
                        .endTime(LocalTime.of(11, 0))
                        .build();
        stale.setStatus(ShowStatus.SCHEDULED);
        when(repository.findByStatusIn(List.of(ShowStatus.SCHEDULED, ShowStatus.RUNNING)))
                .thenReturn(List.of(stale));

        new ShowStatusReconciliationJob(
                        repository,
                        new ShowLifecycleService(
                                clock, repository, mock(AuditBusinessPublisher.class)))
                .reconcileStatuses();

        verify(repository).saveAll(List.of(stale));
    }
}
