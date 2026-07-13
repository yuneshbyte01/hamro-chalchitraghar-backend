package com.chalchitraghar.modules.notifications.repository;

import com.chalchitraghar.modules.notifications.entity.NotificationDelivery;
import com.chalchitraghar.modules.notifications.enums.NotificationChannel;
import com.chalchitraghar.modules.notifications.enums.NotificationDeliveryStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {
    Optional<NotificationDelivery> findByNotificationIdAndChannel(
            Long notificationId, NotificationChannel channel);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from NotificationDelivery d where d.id = :id")
    Optional<NotificationDelivery> findByIdForUpdate(@Param("id") Long id);

    Page<NotificationDelivery> findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            Collection<NotificationDeliveryStatus> statuses, LocalDateTime now, Pageable pageable);

    Page<NotificationDelivery> findByStatusAndClaimedAtLessThanEqualOrderByClaimedAtAsc(
            NotificationDeliveryStatus status, LocalDateTime staleBefore, Pageable pageable);
}
