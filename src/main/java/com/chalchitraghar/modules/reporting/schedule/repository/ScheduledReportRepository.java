package com.chalchitraghar.modules.reporting.schedule.repository;

import com.chalchitraghar.modules.reporting.schedule.entity.ScheduledReport;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduledReportRepository extends JpaRepository<ScheduledReport, Long> {
    List<ScheduledReport> findByDeletedFalseOrderByNameAsc();

    List<ScheduledReport>
            findByEnabledTrueAndDeletedFalseAndNextRunAtLessThanEqualOrderByNextRunAtAsc(
                    LocalDateTime now, Pageable pageable);
}
