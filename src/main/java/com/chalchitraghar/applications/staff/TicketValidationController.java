package com.chalchitraghar.applications.staff;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.validation.Valid;
import com.chalchitraghar.modules.tickets.dto.request.TicketScanRequest;
import com.chalchitraghar.modules.tickets.dto.response.TicketScanResponse;
import com.chalchitraghar.modules.tickets.service.TicketValidationService;
import com.chalchitraghar.modules.users.entity.User;
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
 private final TicketValidationService validationService;
 @GetMapping("/tickets/{ticketReference}") @Operation(summary="Get ticket for staff")
 public ResponseEntity<ApiResponse<StaffTicketDetailResponse>> get(@PathVariable String ticketReference){return ResponseEntity.ok(ApiResponse.success("Ticket fetched successfully",service.staffTicket(ticketReference)));}
 @GetMapping("/bookings/{bookingReference}/tickets") @Operation(summary="List booking tickets for staff")
 public ResponseEntity<ApiResponse<List<StaffTicketSummaryResponse>>> booking(@PathVariable String bookingReference){return ResponseEntity.ok(ApiResponse.success("Tickets fetched successfully",service.staffBookingTickets(bookingReference)));}
 @PostMapping("/tickets/scan") @Operation(summary="Scan and check in a ticket",description="Hashes an opaque QR token, atomically locks and validates the ticket, records every attempt, and prevents replay.")
 public ResponseEntity<ApiResponse<TicketScanResponse>> scan(@Valid @RequestBody TicketScanRequest request,
  @RequestHeader(value="X-Device-ID",required=false) String deviceId,@RequestHeader(value="X-Location",required=false) String location,
  @RequestHeader(value="X-Request-ID",required=false) String requestId){var user=(User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();return ResponseEntity.ok(ApiResponse.success("Ticket validation completed",validationService.scan(request,user,deviceId,location,requestId)));}
}
