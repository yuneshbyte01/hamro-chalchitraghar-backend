package com.chalchitraghar.applications.admin;

import com.chalchitraghar.modules.tickets.dto.request.TicketOperationRequest;
import com.chalchitraghar.modules.tickets.dto.response.*;
import com.chalchitraghar.modules.tickets.service.TicketOperationsService;
import com.chalchitraghar.modules.tickets.service.TicketService;
import com.chalchitraghar.modules.users.entity.User;
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
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Tickets", description = "Read-only administrative ticket lookup")
@SecurityRequirement(name = "bearerAuth")
public class AdminTicketController {
    private final TicketService service;
    private final TicketOperationsService operations;
    private final com.chalchitraghar.modules.tickets.service.TicketQueryService queries;

    @GetMapping("/tickets/{ticketReference}")
    @Operation(summary = "Get ticket for admin")
    public ResponseEntity<ApiResponse<AdminTicketDetailResponse>> get(
            @PathVariable String ticketReference) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Ticket fetched successfully", service.adminTicket(ticketReference)));
    }

    @GetMapping("/bookings/{bookingReference}/tickets")
    @Operation(summary = "List booking tickets for admin")
    public ResponseEntity<ApiResponse<List<AdminTicketSummaryResponse>>> booking(
            @PathVariable String bookingReference) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Tickets fetched successfully",
                        service.adminBookingTickets(bookingReference)));
    }

    @PostMapping("/tickets/{ticketReference}/revoke")
    @Operation(summary = "Revoke an issued ticket")
    public ResponseEntity<ApiResponse<AdminTicketDetailResponse>> revoke(
            @PathVariable String ticketReference,
            @Valid @RequestBody TicketOperationRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Ticket revoked successfully",
                        operations.revoke(ticketReference, request.reason(), admin())));
    }

    @PostMapping("/tickets/{ticketReference}/reissue")
    @Operation(summary = "Rotate an issued ticket QR token")
    public ResponseEntity<ApiResponse<AdminTicketDetailResponse>> reissue(
            @PathVariable String ticketReference,
            @Valid @RequestBody(required = false) TicketOperationRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Ticket reissued successfully",
                        operations.reissue(
                                ticketReference,
                                request == null ? null : request.reason(),
                                admin())));
    }

    private User admin() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @GetMapping("/tickets")
    @Operation(summary = "Search tickets for administration")
    public ResponseEntity<
                    ApiResponse<
                            com.chalchitraghar.shared.response.PageResponse<
                                    AdminTicketSummaryResponse>>>
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
                    @RequestParam(required = false) Long customerId) {
        var c =
                new com.chalchitraghar.modules.tickets.dto.request.TicketSearchCriteria(
                        search,
                        status,
                        showId,
                        movieId,
                        hallId,
                        null,
                        customerId,
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
                        queries.admin(c, page, size, sortBy, sortDir)));
    }

    @GetMapping("/tickets/metrics")
    @Operation(summary = "Ticket operational metrics")
    public ResponseEntity<
                    ApiResponse<
                            com.chalchitraghar.modules.tickets.dto.response.TicketMetricsResponse>>
            metrics() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Ticket metrics fetched successfully",
                        queries.metrics(
                                new com.chalchitraghar.modules.tickets.dto.request
                                        .TicketSearchCriteria(
                                        null, null, null, null, null, null, null, null, null, null,
                                        null, null, null, null, null))));
    }

    @GetMapping("/tickets/validation-summary")
    @Operation(summary = "Validation result summary")
    public ResponseEntity<ApiResponse<java.util.Map<String, Long>>> summary() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Validation summary fetched successfully", queries.validationSummary()));
    }

    @GetMapping("/tickets/inconsistencies")
    @Operation(summary = "Detect ticket consistency issues")
    public ResponseEntity<
                    ApiResponse<
                            java.util.List<
                                    com.chalchitraghar.modules.tickets.dto.response
                                            .TicketConsistencyIssue>>>
            inconsistencies() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Ticket inconsistencies fetched successfully", queries.inconsistencies()));
    }
}
