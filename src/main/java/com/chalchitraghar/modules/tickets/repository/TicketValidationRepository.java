package com.chalchitraghar.modules.tickets.repository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import com.chalchitraghar.modules.tickets.entity.TicketValidation;
public interface TicketValidationRepository extends JpaRepository<TicketValidation,Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<TicketValidation> {
 @EntityGraph(attributePaths={"validatedBy"}) List<TicketValidation> findByTicketIdOrderByValidationTimeDesc(Long ticketId);
 List<TicketValidation> findByValidationTimeBetweenOrderByValidationTimeDesc(LocalDateTime from,LocalDateTime to);
 long countByResult(com.chalchitraghar.modules.tickets.enums.ValidationResult result);
 long countByResultNot(com.chalchitraghar.modules.tickets.enums.ValidationResult result);
}
