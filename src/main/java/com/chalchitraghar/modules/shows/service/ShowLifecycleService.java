package com.chalchitraghar.modules.shows.service;

import com.chalchitraghar.modules.audit.service.AuditBusinessPublisher;
import com.chalchitraghar.modules.halls.enums.Status;
import com.chalchitraghar.modules.movies.enums.MovieStatus;
import com.chalchitraghar.modules.shows.entity.Show;
import com.chalchitraghar.modules.shows.enums.ShowStatus;
import com.chalchitraghar.modules.shows.repository.ShowRepository;
import com.chalchitraghar.shared.exception.ShowConflictException;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Authoritative time-based lifecycle and booking eligibility policy for shows. */
@Service
@RequiredArgsConstructor
public class ShowLifecycleService {

    private final Clock clock;
    private final ShowRepository showRepository;
    private final AuditBusinessPublisher audit;

    public ShowStatus effectiveStatus(Show show) {
        if (show.getStatus() == ShowStatus.CANCELLED || show.getStatus() == ShowStatus.COMPLETED) {
            return show.getStatus();
        }
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime start = LocalDateTime.of(show.getShowDate(), show.getShowTime());
        LocalDateTime end = LocalDateTime.of(show.getShowDate(), show.getEndTime());
        if (!now.isBefore(end)) {
            return ShowStatus.COMPLETED;
        }
        if (!now.isBefore(start)) {
            return ShowStatus.RUNNING;
        }
        return ShowStatus.SCHEDULED;
    }

    public boolean reconcile(Show show) {
        ShowStatus effective = effectiveStatus(show);
        if ((show.getStatus() == ShowStatus.SCHEDULED && effective == ShowStatus.RUNNING)
                || ((show.getStatus() == ShowStatus.SCHEDULED
                                || show.getStatus() == ShowStatus.RUNNING)
                        && effective == ShowStatus.COMPLETED)) {
            ShowStatus before = show.getStatus();
            show.setStatus(effective);
            audit.systemShowTransition(show.getId(), before.name(), effective.name());
            return true;
        }
        return false;
    }

    public void assertBookable(Show show) {
        reconcile(show);
        if (effectiveStatus(show) != ShowStatus.SCHEDULED
                || show.getMovie().getStatus() != MovieStatus.NOW_SHOWING
                || show.getHall().getStatus() != Status.ACTIVE) {
            throw new ShowConflictException(
                    "Show is not bookable; it must be scheduled in the future with an active hall and a now-showing movie");
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reconcilePersisted(Long showId) {
        showRepository
                .findById(showId)
                .ifPresent(
                        show -> {
                            if (reconcile(show)) {
                                showRepository.save(show);
                            }
                        });
    }
}
