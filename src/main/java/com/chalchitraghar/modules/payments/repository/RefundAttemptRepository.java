package com.chalchitraghar.modules.payments.repository;

import com.chalchitraghar.modules.payments.entity.RefundAttempt;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface RefundAttemptRepository extends JpaRepository<RefundAttempt, Long> {
    List<RefundAttempt> findByRefundIdOrderByAttemptNumberAsc(Long refundId);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from RefundAttempt a where a.refund.id=:refundId and a.attemptNumber=:number")
    Optional<RefundAttempt> findForUpdate(
            @Param("refundId") Long refundId, @Param("number") int number);
}
