package com.chalchitraghar.modules.reporting.repository;

import com.chalchitraghar.modules.bookings.entity.Booking;
import com.chalchitraghar.modules.movies.enums.MovieStatus;
import com.chalchitraghar.modules.payments.enums.PaymentStatus;
import com.chalchitraghar.modules.payments.enums.RefundStatus;
import com.chalchitraghar.modules.reporting.projection.*;
import com.chalchitraghar.modules.shows.enums.ShowStatus;
import com.chalchitraghar.modules.users.enums.Role;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface ReportingQueryRepository extends Repository<Booking, Long> {
    @Query(
            """
            select b.status as status, count(b) as count
            from Booking b
            where b.bookingTime >= :start and b.bookingTime < :end
            group by b.status
            """)
    List<BookingStatusCountProjection> bookingStatusCounts(
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(
            """
            select p.currency as currency, count(p) as count, coalesce(sum(p.amount), 0) as amount
            from Payment p
            where p.status in :statuses and p.completedAt >= :start and p.completedAt < :end
              and (:currency is null or p.currency = :currency)
            group by p.currency order by p.currency
            """)
    List<PaymentRevenueProjection> paymentRevenue(
            @Param("statuses") Collection<PaymentStatus> statuses,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("currency") String currency);

    @Query(
            """
            select r.currency as currency, count(r) as count, coalesce(sum(r.amount), 0) as amount
            from Refund r
            where r.status = :status and r.processedAt >= :start and r.processedAt < :end
              and (:currency is null or r.currency = :currency)
            group by r.currency order by r.currency
            """)
    List<RefundRevenueProjection> refundRevenue(
            @Param("status") RefundStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("currency") String currency);

    @Query(
            "select count(u) from User u where u.role = :role and u.createdAt >= :start and u.createdAt < :end")
    long registeredUsers(
            @Param("role") Role role,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("select count(m) from Movie m where m.status in :statuses")
    long moviesByStatus(@Param("statuses") Collection<MovieStatus> statuses);

    @Query("select count(s) from Show s where s.status = :status")
    long showsByStatus(@Param("status") ShowStatus status);
}
