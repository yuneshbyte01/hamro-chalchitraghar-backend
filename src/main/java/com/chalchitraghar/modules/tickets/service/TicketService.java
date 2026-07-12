package com.chalchitraghar.modules.tickets.service;
import java.util.List;
import com.chalchitraghar.modules.tickets.dto.response.*;
import com.chalchitraghar.modules.users.entity.User;
public interface TicketService {
 List<CustomerTicketSummaryResponse> customerTickets(User user);
 CustomerTicketDetailResponse customerTicket(String reference, User user);
 List<CustomerTicketSummaryResponse> customerBookingTickets(String bookingReference, User user);
 StaffTicketDetailResponse staffTicket(String reference);
 List<StaffTicketSummaryResponse> staffBookingTickets(String bookingReference);
 AdminTicketDetailResponse adminTicket(String reference);
 List<AdminTicketSummaryResponse> adminBookingTickets(String bookingReference);
 byte[] customerQrPng(String reference,User user);
 CustomerQrDataResponse customerQrData(String reference,User user);
}
