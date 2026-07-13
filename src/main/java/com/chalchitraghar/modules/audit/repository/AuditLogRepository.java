package com.chalchitraghar.modules.audit.repository;

import com.chalchitraghar.modules.audit.entity.AuditLog;
import com.chalchitraghar.modules.audit.enums.AuditAction;
import com.chalchitraghar.modules.audit.enums.AuditActorType;
import com.chalchitraghar.modules.audit.enums.AuditResult;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository
        extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
    boolean existsByEventId(String eventId);

    long countByAction(AuditAction action);

    List<AuditLog> findAllByActionOrderByIdAsc(AuditAction action);

    long countByOccurredAtBetween(LocalDateTime from, LocalDateTime to);

    long countByResultAndOccurredAtBetween(
            AuditResult result, LocalDateTime from, LocalDateTime to);

    long countByActorTypeAndOccurredAtBetween(
            AuditActorType actorType, LocalDateTime from, LocalDateTime to);

    @Query(
            "select a.category, count(a) from AuditLog a where a.occurredAt between :from and :to group by a.category")
    List<Object[]> countCategories(
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(
            "select a.severity, count(a) from AuditLog a where a.occurredAt between :from and :to group by a.severity")
    List<Object[]> countSeverities(
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(
            "select a.actorType, count(a) from AuditLog a where a.occurredAt between :from and :to group by a.actorType")
    List<Object[]> countActorTypes(
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(
            "select a.action, count(a) from AuditLog a where a.occurredAt between :from and :to group by a.action order by count(a) desc")
    List<Object[]> countActions(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            org.springframework.data.domain.Pageable pageable);
}
