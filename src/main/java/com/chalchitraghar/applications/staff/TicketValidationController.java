package com.chalchitraghar.applications.staff;

import com.chalchitraghar.modules.tickets.dto.request.TicketScanRequest;
import com.chalchitraghar.modules.tickets.dto.response.*;
import com.chalchitraghar.modules.tickets.dto.response.TicketScanResponse;
import com.chalchitraghar.modules.tickets.service.TicketService;
import com.chalchitraghar.modules.tickets.service.TicketValidationService;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.shared.audit.RequestAuditContextHolder;
import com.chalchitraghar.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
@Tag(
        name = "Staff Tickets",
        description = "Read-only operational ticket lookup; scanning and check-in are deferred")
@SecurityRequirement(name = "bearerAuth")
public class TicketValidationController {
    private final TicketService service;
    private final TicketValidationService validationService;
    private final com.chalchitraghar.modules.tickets.service.TicketQueryService queries;

    @GetMapping("/tickets/{ticketReference}")
    @Operation(summary = "Get ticket for staff")
    public ResponseEntity<ApiResponse<StaffTicketDetailResponse>> get(
            @PathVariable String ticketReference) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Ticket fetched successfully", service.staffTicket(ticketReference)));
    }

    @GetMapping("/bookings/{bookingReference}/tickets")
    @Operation(summary = "List booking tickets for staff")
    public ResponseEntity<ApiResponse<List<StaffTicketSummaryResponse>>> booking(
            @PathVariable String bookingReference) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Tickets fetched successfully",
                        service.staffBookingTickets(bookingReference)));
    }

    @PostMapping("/tickets/scan")
    @Operation(
            summary = "Scan and check in a ticket",
            description =
                    "Hashes an opaque QR token, atomically locks and validates the ticket, records every attempt, and prevents replay.")
    public ResponseEntity<ApiResponse<TicketScanResponse>> scan(
            @Valid @RequestBody TicketScanRequest request,
            @RequestHeader(value = "X-Device-ID", required = false) String deviceId,
            @RequestHeader(value = "X-Location", required = false) String location) {
        var user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String requestId = RequestAuditContextHolder.current().map(c -> c.requestId()).orElse(null);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Ticket validation completed",
                        validationService.scan(request, user, deviceId, location, requestId)));
    }

    @GetMapping("/tickets")
    @Operation(summary = "Search tickets for admission operations")
    public ResponseEntity<
                    ApiResponse<
                            com.chalchitraghar.shared.response.PageResponse<
                                    StaffTicketSummaryResponse>>>
            list(
                    @RequestParam(defaultValue = "0") int page,
                    @RequestParam(defaultValue = "20") int size,
                    @RequestParam(defaultValue = "issuedAt") String sortBy,
                    @RequestParam(defaultValue = "desc") String sortDir,
                    @RequestParam(required = false) String search,
                    @RequestParam(required = false) String status,
                    @RequestParam(required = false) Long showId,
                    @RequestParam(required = false) Long movieId,
                    @RequestParam(required = false) Long hallId,
                    @RequestParam(required = false) String bookingReference) {
        var c =
                new com.chalchitraghar.modules.tickets.dto.request.TicketSearchCriteria(
                        search,
                        status,
                        showId,
                        movieId,
                        hallId,
                        bookingReference,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Tickets fetched successfully",
                        queries.staff(c, page, size, sortBy, sortDir)));
    }
}
