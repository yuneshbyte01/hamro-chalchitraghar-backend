package com.chalchitraghar.modules.tickets.repository;
import java.util.*; import org.springframework.data.domain.Pageable; import org.springframework.data.jpa.repository.JpaRepository;
import com.chalchitraghar.modules.tickets.entity.TicketDelivery; import com.chalchitraghar.modules.tickets.enums.*;
public interface TicketDeliveryRepository extends JpaRepository<TicketDelivery,Long>{
 Optional<TicketDelivery> findByBookingIdAndChannel(Long bookingId,TicketDeliveryChannel channel);
 List<TicketDelivery> findByStatusInAndAttemptCountLessThanOrderByCreatedAtAsc(List<TicketDeliveryStatus> statuses,int max,Pageable pageable);
}
