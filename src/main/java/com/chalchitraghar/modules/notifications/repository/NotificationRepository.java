package com.chalchitraghar.modules.notifications.repository;

import com.chalchitraghar.modules.notifications.entity.Notification;
import com.chalchitraghar.modules.notifications.enums.NotificationChannel;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository
        extends JpaRepository<Notification, Long>, JpaSpecificationExecutor<Notification> {
    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    Optional<Notification> findByUserIdAndEventKeyAndChannel(
            Long userId, String eventKey, NotificationChannel channel);

    boolean existsByUserIdAndEventKeyAndChannel(
            Long userId, String eventKey, NotificationChannel channel);

    long countByUserIdAndChannelAndReadFalse(Long userId, NotificationChannel channel);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
            update Notification n
               set n.read = true, n.readAt = :readAt, n.updatedAt = :readAt
             where n.user.id = :userId
               and n.channel = :channel
               and n.read = false
            """)
    int markAllRead(
            @Param("userId") Long userId,
            @Param("channel") NotificationChannel channel,
            @Param("readAt") LocalDateTime readAt);
}
