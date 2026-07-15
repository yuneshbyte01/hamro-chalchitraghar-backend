package com.chalchitraghar.applications.customer;

import com.chalchitraghar.modules.payments.dto.request.CustomerRefundFilter;
import com.chalchitraghar.modules.payments.dto.response.*;
import com.chalchitraghar.modules.payments.enums.*;
import com.chalchitraghar.modules.payments.service.RefundService;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.shared.response.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
@Tag(
        name = "Customer Refunds",
        description =
                "Owner-safe Refund-1 intent reads; REQUESTED does not mean money was returned")
@SecurityRequirement(name = "bearerAuth")
public class RefundController {
    private final RefundService service;

    @GetMapping("/refunds")
    @Operation(
            summary = "List my refund intents",
            description =
                    "Full-refund intents only. No request or processing operation is exposed.")
    public ResponseEntity<ApiResponse<PageResponse<CustomerRefundSummaryResponse>>> list(
            @RequestParam(required = false) RefundStatus status,
            @RequestParam(required = false) RefundReason reason,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime requestedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime requestedTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var filter = new CustomerRefundFilter(status, reason, requestedFrom, requestedTo);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Refunds fetched successfully",
                        service.getCustomerRefunds(filter, page, size, user())));
    }

    @GetMapping("/refunds/{reference}")
    @Operation(
            summary = "Get my refund intent",
            description = "Unknown and non-owned references return 404.")
    public ResponseEntity<ApiResponse<CustomerRefundDetailResponse>> get(
            @PathVariable String reference) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Refund fetched successfully",
                        service.getCustomerRefund(reference, user())));
    }

    @GetMapping("/bookings/{reference}/refunds")
    @Operation(summary = "List refund intents for my booking")
    public ResponseEntity<ApiResponse<List<CustomerRefundSummaryResponse>>> byBooking(
            @PathVariable String reference) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Refunds fetched successfully",
                        service.getCustomerBookingRefunds(reference, user())));
    }

    private User user() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
