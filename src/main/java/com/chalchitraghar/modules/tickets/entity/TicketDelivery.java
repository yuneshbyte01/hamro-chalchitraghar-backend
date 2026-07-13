package com.chalchitraghar.modules.tickets.entity;

import com.chalchitraghar.modules.bookings.entity.Booking;
import com.chalchitraghar.modules.tickets.enums.*;
import com.chalchitraghar.shared.GenericEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
        name = "ticket_deliveries",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_ticket_deliveries_booking_channel",
                        columnNames = {"booking_id", "channel"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketDelivery extends GenericEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TicketDeliveryChannel channel;

    @Column(nullable = false)
    private String recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TicketDeliveryStatus status;

    @Column(nullable = false)
    private int attemptCount;

    private LocalDateTime lastAttemptAt;
    private LocalDateTime sentAt;

    @Column(length = 500)
    private String failureMessage;
}
