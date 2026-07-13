package com.chalchitraghar.modules.tickets.service;

import com.chalchitraghar.modules.users.entity.User;

public interface TicketPdfService {
    byte[] customerTicket(String reference, User user);

    byte[] customerBooking(String bookingReference, User user);

    byte[] bookingPdf(Long bookingId);
}
