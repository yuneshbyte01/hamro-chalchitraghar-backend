package com.chalchitraghar.modules.tickets.entity;

import com.chalchitraghar.modules.tickets.enums.ValidationResult;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.shared.GenericEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "ticket_validations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketValidation extends GenericEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "validated_by_user_id", nullable = false)
    @NotNull
    private User validatedBy;

    @Column(nullable = false)
    @NotNull
    private LocalDateTime validationTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @NotNull
    private ValidationResult result;

    @Column(length = 500)
    private String reason;

    @Column(length = 200)
    private String deviceId;

    @Column(length = 200)
    private String location;

    @Column(length = 200)
    private String requestId;
}
