package com.chalchitraghar.applications.admin;

import com.chalchitraghar.modules.notifications.dto.response.*;
import com.chalchitraghar.modules.notifications.enums.*;
import com.chalchitraghar.modules.notifications.service.AdminNotificationOperationsService;
import com.chalchitraghar.shared.response.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(
        name = "Admin Notifications",
        description = "ADMIN-only notification and masked delivery diagnostics")
public class AdminNotificationController {
    private final AdminNotificationOperationsService service;

    @GetMapping("/notifications")
    @Operation(
            summary = "Search notifications",
            description =
                    "Bounded operational search; sensitive delivery and payload data is omitted or masked")
    public ResponseEntity<ApiResponse<PageResponse<AdminNotificationSummaryResponse>>>
            notifications(
                    @RequestParam(defaultValue = "0") int page,
                    @RequestParam(defaultValue = "20") int size,
                    @RequestParam(required = false) Long userId,
                    @RequestParam(required = false) String email,
                    @RequestParam(required = false) NotificationType type,
                    @RequestParam(required = false) NotificationChannel channel,
                    @RequestParam(required = false) Boolean read,
                    @RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                            LocalDateTime occurredFrom,
                    @RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                            LocalDateTime occurredTo,
                    @RequestParam(required = false) NotificationDeliveryStatus deliveryStatus,
                    @RequestParam(required = false) String eventKey) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Notifications fetched",
                        service.notifications(
                                page,
                                size,
                                userId,
                                email,
                                type,
                                channel,
                                read,
                                occurredFrom,
                                occurredTo,
                                deliveryStatus,
                                eventKey)));
    }

    @GetMapping("/notifications/{id}")
    @Operation(summary = "Get notification operational detail")
    public ResponseEntity<ApiResponse<AdminNotificationDetailResponse>> notification(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success("Notification fetched", service.notification(id)));
    }

    @GetMapping("/notification-deliveries")
    @Operation(summary = "Search notification email deliveries")
    public ResponseEntity<ApiResponse<PageResponse<AdminNotificationDeliverySummaryResponse>>>
            deliveries(
                    @RequestParam(defaultValue = "0") int page,
                    @RequestParam(defaultValue = "20") int size,
                    @RequestParam(required = false) NotificationDeliveryStatus status,
                    @RequestParam(required = false) NotificationType notificationType,
                    @RequestParam(required = false) Integer minAttempts,
                    @RequestParam(required = false) Integer maxAttempts) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Notification deliveries fetched",
                        service.deliveries(
                                page, size, status, notificationType, minAttempts, maxAttempts)));
    }

    @GetMapping("/notification-deliveries/{id}")
    @Operation(summary = "Get notification delivery detail")
    public ResponseEntity<ApiResponse<AdminNotificationDeliveryDetailResponse>> delivery(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success("Notification delivery fetched", service.delivery(id)));
    }

    @PostMapping("/notification-deliveries/{id}/retry")
    @Operation(
            summary = "Queue an eligible FAILED delivery for retry",
            description = "SENT, PROCESSING, SKIPPED, and EXHAUSTED deliveries are rejected")
    public ResponseEntity<ApiResponse<ManualDeliveryRetryResponse>> retry(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success("Notification delivery queued", service.retry(id)));
    }
}
