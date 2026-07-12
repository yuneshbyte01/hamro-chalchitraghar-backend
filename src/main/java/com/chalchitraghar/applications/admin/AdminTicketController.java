package com.chalchitraghar.applications.admin;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.chalchitraghar.modules.tickets.dto.response.*;
import com.chalchitraghar.modules.tickets.service.TicketService;
import com.chalchitraghar.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
@RestController @RequestMapping("/api/admin") @RequiredArgsConstructor
@Tag(name="Admin Tickets",description="Read-only administrative ticket lookup") @SecurityRequirement(name="bearerAuth")
public class AdminTicketController {
 private final TicketService service;
 @GetMapping("/tickets/{ticketReference}") @Operation(summary="Get ticket for admin")
 public ResponseEntity<ApiResponse<AdminTicketDetailResponse>> get(@PathVariable String ticketReference){return ResponseEntity.ok(ApiResponse.success("Ticket fetched successfully",service.adminTicket(ticketReference)));}
 @GetMapping("/bookings/{bookingReference}/tickets") @Operation(summary="List booking tickets for admin")
 public ResponseEntity<ApiResponse<List<AdminTicketSummaryResponse>>> booking(@PathVariable String bookingReference){return ResponseEntity.ok(ApiResponse.success("Tickets fetched successfully",service.adminBookingTickets(bookingReference)));}
}
