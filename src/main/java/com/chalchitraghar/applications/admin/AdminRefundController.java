package com.chalchitraghar.applications.admin;

import com.chalchitraghar.modules.payments.dto.request.AdminRefundFilter;
import com.chalchitraghar.modules.payments.dto.response.*;
import com.chalchitraghar.modules.payments.enums.*;
import com.chalchitraghar.modules.payments.service.RefundService;
import com.chalchitraghar.shared.response.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/refunds")
@RequiredArgsConstructor
@Tag(
        name = "Admin Refunds",
        description =
                "ADMIN-only Refund-1 intent visibility; no approval, processing, retry, or provider integration")
@SecurityRequirement(name = "bearerAuth")
public class AdminRefundController {
    private final RefundService service;

    @GetMapping
    @Operation(
            summary = "Search refund intents",
            description = "Stable newest-first pagination over full-refund intents.")
    public ResponseEntity<ApiResponse<PageResponse<AdminRefundSummaryResponse>>> list(
            @RequestParam(required = false) String refundReference,
            @RequestParam(required = false) String paymentReference,
            @RequestParam(required = false) String bookingReference,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String customerEmail,
            @RequestParam(required = false) RefundStatus status,
            @RequestParam(required = false) RefundReason reason,
            @RequestParam(required = false) RefundType type,
            @RequestParam(required = false) RefundMethod method,
            @RequestParam(required = false) PaymentProvider provider,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime requestedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime requestedTo,
            @RequestParam(required = false) BigDecimal amountFrom,
            @RequestParam(required = false) BigDecimal amountTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var filter =
                new AdminRefundFilter(
                        refundReference,
                        paymentReference,
                        bookingReference,
                        customerId,
                        customerEmail,
                        status,
                        reason,
                        type,
                        method,
                        provider,
                        requestedFrom,
                        requestedTo,
                        amountFrom,
                        amountTo);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Refunds fetched successfully",
                        service.getAdminRefunds(filter, page, size)));
    }

    @GetMapping("/{reference}")
    @Operation(
            summary = "Get refund intent detail",
            description = "Returns sanitized financial and ticket-status context.")
    public ResponseEntity<ApiResponse<AdminRefundDetailResponse>> get(
            @PathVariable String reference) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Refund fetched successfully", service.getAdminRefund(reference)));
    }
}
