package com.chalchitraghar.modules.audit.repository;

import com.chalchitraghar.modules.audit.entity.AuditLog;
import com.chalchitraghar.modules.audit.enums.AuditAction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditLogRepository
        extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
    boolean existsByEventId(String eventId);

    long countByAction(AuditAction action);

    List<AuditLog> findAllByActionOrderByIdAsc(AuditAction action);
}
