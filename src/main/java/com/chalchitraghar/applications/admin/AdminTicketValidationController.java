package com.chalchitraghar.applications.admin;

import com.chalchitraghar.modules.tickets.dto.response.TicketValidationHistoryResponse;
import com.chalchitraghar.modules.tickets.service.TicketValidationQueryService;
import com.chalchitraghar.shared.response.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/ticket-validations")
@RequiredArgsConstructor
@Tag(name = "Admin Ticket Validations")
@SecurityRequirement(name = "bearerAuth")
public class AdminTicketValidationController {
    private final TicketValidationQueryService service;

    @GetMapping
    @Operation(summary = "Search ticket validation history")
    public ResponseEntity<ApiResponse<PageResponse<TicketValidationHistoryResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String ticketReference,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) Long validatedBy,
            @RequestParam(required = false) Long showId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime dateTo,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String location) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Ticket validations fetched successfully",
                        service.search(
                                page,
                                size,
                                ticketReference,
                                result,
                                validatedBy,
                                showId,
                                dateFrom,
                                dateTo,
                                deviceId,
                                location)));
    }
}
