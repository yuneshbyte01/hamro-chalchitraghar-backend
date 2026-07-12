package com.chalchitraghar.modules.tickets.service;
import java.util.List;
import com.chalchitraghar.modules.bookings.entity.Booking;
import com.chalchitraghar.modules.tickets.entity.Ticket;
public interface TicketIssuanceService {
    List<Ticket> issueTicketsForConfirmedBooking(Booking booking);
    List<Ticket> backfillConfirmedBooking(Long bookingId);
}
