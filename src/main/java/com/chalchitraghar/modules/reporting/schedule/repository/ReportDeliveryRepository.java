package com.chalchitraghar.modules.reporting.schedule.repository;

import com.chalchitraghar.modules.reporting.enums.ReportDeliveryStatus;
import com.chalchitraghar.modules.reporting.schedule.entity.ReportDelivery;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportDeliveryRepository extends JpaRepository<ReportDelivery, Long> {
    Optional<ReportDelivery> findByIdempotencyKey(String key);

    List<ReportDelivery>
            findByStatusAndAttemptCountLessThanAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
                    ReportDeliveryStatus status,
                    int attempts,
                    LocalDateTime now,
                    Pageable pageable);
}
