package com.chalchitraghar.modules.tickets.repository;

import com.chalchitraghar.modules.tickets.entity.TicketDelivery;
import com.chalchitraghar.modules.tickets.enums.*;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketDeliveryRepository extends JpaRepository<TicketDelivery, Long> {
    Optional<TicketDelivery> findByBookingIdAndChannel(
            Long bookingId, TicketDeliveryChannel channel);

    List<TicketDelivery> findByStatusInAndAttemptCountLessThanOrderByCreatedAtAsc(
            List<TicketDeliveryStatus> statuses, int max, Pageable pageable);
}
