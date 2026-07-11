package com.chalchitraghar;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import com.chalchitraghar.modules.shows.entity.Show;
import com.chalchitraghar.modules.shows.enums.ShowStatus;
import com.chalchitraghar.modules.shows.service.ShowLifecycleService;
import com.chalchitraghar.modules.shows.repository.ShowRepository;
import static org.mockito.Mockito.mock;

class ShowLifecycleServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kathmandu");

    @Test
    void calculatesAndReconcilesEffectiveLifecycleWithoutChangingTerminalStatuses() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-11T06:30:00Z"), ZONE); // 12:15 local
        ShowLifecycleService service = new ShowLifecycleService(clock, mock(ShowRepository.class));

        Show future = show(ShowStatus.SCHEDULED, LocalTime.of(13, 0), LocalTime.of(15, 0));
        Show started = show(ShowStatus.SCHEDULED, LocalTime.of(11, 0), LocalTime.of(13, 0));
        Show ended = show(ShowStatus.RUNNING, LocalTime.of(9, 0), LocalTime.of(11, 0));
        Show cancelled = show(ShowStatus.CANCELLED, LocalTime.of(9, 0), LocalTime.of(11, 0));
        Show completed = show(ShowStatus.COMPLETED, LocalTime.of(13, 0), LocalTime.of(15, 0));

        assertThat(service.effectiveStatus(future)).isEqualTo(ShowStatus.SCHEDULED);
        assertThat(service.reconcile(future)).isFalse();
        assertThat(service.reconcile(started)).isTrue();
        assertThat(started.getStatus()).isEqualTo(ShowStatus.RUNNING);
        assertThat(service.reconcile(ended)).isTrue();
        assertThat(ended.getStatus()).isEqualTo(ShowStatus.COMPLETED);
        assertThat(service.reconcile(cancelled)).isFalse();
        assertThat(service.reconcile(completed)).isFalse();
    }

    private Show show(ShowStatus status, LocalTime start, LocalTime end) {
        Show show = Show.builder().showDate(LocalDate.of(2026, 7, 11))
                .showTime(start).endTime(end).build();
        show.setStatus(status);
        return show;
    }
}
