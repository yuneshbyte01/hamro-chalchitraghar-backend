package com.chalchitraghar.modules.tickets.repository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import com.chalchitraghar.modules.tickets.entity.TicketValidation;
public interface TicketValidationRepository extends JpaRepository<TicketValidation,Long> {
 @EntityGraph(attributePaths={"validatedBy"}) List<TicketValidation> findByTicketIdOrderByValidationTimeDesc(Long ticketId);
 List<TicketValidation> findByValidationTimeBetweenOrderByValidationTimeDesc(LocalDateTime from,LocalDateTime to);
}
