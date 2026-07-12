package com.chalchitraghar.modules.tickets.service.impl;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.chalchitraghar.modules.bookings.repository.BookingRepository;
import com.chalchitraghar.modules.tickets.dto.response.*;
import com.chalchitraghar.modules.tickets.entity.Ticket;
import com.chalchitraghar.modules.tickets.mapper.TicketMapper;
import com.chalchitraghar.modules.tickets.repository.TicketRepository;
import com.chalchitraghar.modules.tickets.service.TicketService;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class TicketServiceImpl implements TicketService {
 private final TicketRepository tickets; private final BookingRepository bookings; private final TicketMapper mapper; private final com.chalchitraghar.modules.tickets.service.QrTokenService qrTokens; private final com.chalchitraghar.modules.tickets.service.QrImageService qrImages; private final com.chalchitraghar.modules.tickets.repository.TicketValidationRepository validations;
 public List<CustomerTicketSummaryResponse> customerTickets(User u){return tickets.findByBookingUserIdOrderByIssuedAtDesc(u.getId()).stream().map(mapper::toCustomerSummary).toList();}
 public CustomerTicketDetailResponse customerTicket(String r,User u){return mapper.toCustomerDetail(tickets.findByTicketReferenceAndBookingUserId(norm(r),u.getId()).orElseThrow(()->notFound(r)));}
 public List<CustomerTicketSummaryResponse> customerBookingTickets(String r,User u){var b=bookings.findByBookingReference(norm(r)).filter(x->x.getUser().getId().equals(u.getId())).orElseThrow(()->bookingNotFound(r));return bySeat(tickets.findByBookingIdOrderByIssuedAtAsc(b.getId())).stream().map(mapper::toCustomerSummary).toList();}
 public StaffTicketDetailResponse staffTicket(String r){Ticket t=find(r);return mapper.toStaffDetail(t,validations.findByTicketIdOrderByValidationTimeDesc(t.getId()));}
 public List<StaffTicketSummaryResponse> staffBookingTickets(String r){ensureBooking(r);return bySeat(tickets.findByBookingBookingReferenceOrderByIssuedAtAsc(norm(r))).stream().map(mapper::toStaffSummary).toList();}
 public AdminTicketDetailResponse adminTicket(String r){Ticket t=find(r);return mapper.toAdminDetail(t,validations.findByTicketIdOrderByValidationTimeDesc(t.getId()));}
 public List<AdminTicketSummaryResponse> adminBookingTickets(String r){ensureBooking(r);return bySeat(tickets.findByBookingBookingReferenceOrderByIssuedAtAsc(norm(r))).stream().map(mapper::toAdminSummary).toList();}
 public byte[] customerQrPng(String r,User u){Ticket t=owned(r,u);return qrImages.generatePng(qrTokens.decryptToken(t));}
 public CustomerQrDataResponse customerQrData(String r,User u){Ticket t=owned(r,u);return new CustomerQrDataResponse(t.getTicketReference(),t.getQrTokenVersion(),t.getQrIssuedAt());}
 private Ticket owned(String r,User u){return tickets.findByTicketReferenceAndBookingUserId(norm(r),u.getId()).orElseThrow(()->notFound(r));}
 private Ticket find(String r){return tickets.findByTicketReference(norm(r)).orElseThrow(()->notFound(r));}
 private void ensureBooking(String r){if(bookings.findByBookingReference(norm(r)).isEmpty())throw bookingNotFound(r);}
 private List<Ticket> bySeat(List<Ticket> list){return list.stream().sorted(Comparator.comparing(t->t.getBookingSeat().getSeat().getPositionIndex())).toList();}
 private String norm(String r){return r==null?"":r.trim().toUpperCase();}
 private ResourceNotFoundException notFound(String r){return new ResourceNotFoundException("Ticket not found with reference: "+r);}
 private ResourceNotFoundException bookingNotFound(String r){return new ResourceNotFoundException("Booking not found with reference: "+r);}
}
