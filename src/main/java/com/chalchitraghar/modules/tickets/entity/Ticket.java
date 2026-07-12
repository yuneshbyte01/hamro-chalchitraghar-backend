package com.chalchitraghar.modules.tickets.entity;

import java.time.LocalDateTime;

import com.chalchitraghar.modules.bookings.entity.Booking;
import com.chalchitraghar.modules.bookings.entity.BookingSeat;
import com.chalchitraghar.modules.tickets.enums.TicketStatus;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.shared.GenericEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tickets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ticket extends GenericEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    @NotNull
    private Booking booking;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_seat_id", nullable = false, unique = true)
    @NotNull
    private BookingSeat bookingSeat;

    @Column(name = "ticket_reference", nullable = false, unique = true, updatable = false, length = 50)
    @NotNull
    private String ticketReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @NotNull
    private TicketStatus status;

    @Column(name = "issued_at", nullable = false)
    @NotNull
    private LocalDateTime issuedAt;

    private LocalDateTime checkedInAt;
    private LocalDateTime revokedAt;
    private LocalDateTime expiredAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checked_in_by_user_id")
    private User checkedInBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revoked_by_user_id")
    private User revokedBy;

    @Column(length = 500)
    private String revocationReason;
    private Integer qrTokenVersion;

    @Column(nullable = false, length = 100)
    private String qrKeyId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String qrTokenEncrypted;

    @Column(nullable = false, unique = true, length = 128)
    private String qrTokenHash;

    @Column(nullable = false)
    private LocalDateTime qrIssuedAt;
}
