package com.chalchitraghar.applications.staff;
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
@RestController @RequestMapping("/api/staff") @RequiredArgsConstructor
@Tag(name="Staff Tickets",description="Read-only operational ticket lookup; scanning and check-in are deferred") @SecurityRequirement(name="bearerAuth")
public class TicketValidationController {
 private final TicketService service;
 @GetMapping("/tickets/{ticketReference}") @Operation(summary="Get ticket for staff")
 public ResponseEntity<ApiResponse<StaffTicketDetailResponse>> get(@PathVariable String ticketReference){return ResponseEntity.ok(ApiResponse.success("Ticket fetched successfully",service.staffTicket(ticketReference)));}
 @GetMapping("/bookings/{bookingReference}/tickets") @Operation(summary="List booking tickets for staff")
 public ResponseEntity<ApiResponse<List<StaffTicketSummaryResponse>>> booking(@PathVariable String bookingReference){return ResponseEntity.ok(ApiResponse.success("Tickets fetched successfully",service.staffBookingTickets(bookingReference)));}
}
