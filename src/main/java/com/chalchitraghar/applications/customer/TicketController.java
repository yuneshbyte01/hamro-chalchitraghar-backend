package com.chalchitraghar.applications.customer;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.chalchitraghar.modules.tickets.dto.response.*;
import com.chalchitraghar.modules.tickets.service.TicketService;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.shared.exception.AuthenticationException;
import com.chalchitraghar.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController @RequestMapping("/api/customer") @RequiredArgsConstructor
@Tag(name="Customer Tickets",description="Owner-only per-seat ticket reads; QR and check-in are deferred")
@SecurityRequirement(name="bearerAuth")
public class TicketController {
 private final TicketService service;
 @GetMapping("/tickets") @Operation(summary="List my tickets",description="Returns only tickets owned by the authenticated user, newest first.")
 public ResponseEntity<ApiResponse<List<CustomerTicketSummaryResponse>>> list(){return ResponseEntity.ok(ApiResponse.success("Tickets fetched successfully",service.customerTickets(user())));}
 @GetMapping("/tickets/{ticketReference}") @Operation(summary="Get my ticket",description="Unknown and non-owned references return not found.")
 public ResponseEntity<ApiResponse<CustomerTicketDetailResponse>> get(@PathVariable String ticketReference){return ResponseEntity.ok(ApiResponse.success("Ticket fetched successfully",service.customerTicket(ticketReference,user())));}
 @GetMapping("/bookings/{bookingReference}/tickets") @Operation(summary="List tickets for my booking",description="One issued ticket per booked seat, ordered by seat position.")
 public ResponseEntity<ApiResponse<List<CustomerTicketSummaryResponse>>> booking(@PathVariable String bookingReference){return ResponseEntity.ok(ApiResponse.success("Tickets fetched successfully",service.customerBookingTickets(bookingReference,user())));}
 private User user(){var a=SecurityContextHolder.getContext().getAuthentication();if(a==null||!(a.getPrincipal() instanceof User u))throw new AuthenticationException("User not authenticated");return u;}
}
