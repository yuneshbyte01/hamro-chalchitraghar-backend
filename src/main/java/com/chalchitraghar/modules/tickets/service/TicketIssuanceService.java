package com.chalchitraghar.modules.tickets.service;

import com.chalchitraghar.modules.bookings.entity.Booking;
import com.chalchitraghar.modules.tickets.entity.Ticket;
import java.util.List;

public interface TicketIssuanceService {
    List<Ticket> issueTicketsForConfirmedBooking(Booking booking);

    List<Ticket> backfillConfirmedBooking(Long bookingId);
}
