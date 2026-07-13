package com.chalchitraghar.applications.customer;

import com.chalchitraghar.modules.notifications.dto.request.NotificationSearchCriteria;
import com.chalchitraghar.modules.notifications.dto.response.CustomerNotificationDetailResponse;
import com.chalchitraghar.modules.notifications.dto.response.CustomerNotificationSummaryResponse;
import com.chalchitraghar.modules.notifications.dto.response.ReadAllNotificationsResponse;
import com.chalchitraghar.modules.notifications.dto.response.UnreadNotificationCountResponse;
import com.chalchitraghar.modules.notifications.enums.NotificationChannel;
import com.chalchitraghar.modules.notifications.enums.NotificationType;
import com.chalchitraghar.modules.notifications.service.NotificationService;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.shared.exception.AuthenticationException;
import com.chalchitraghar.shared.response.ApiResponse;
import com.chalchitraghar.shared.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer/notifications")
@RequiredArgsConstructor
@Tag(
        name = "Customer Notifications",
        description =
                "Owner-only in-app notifications created after committed registration, booking, payment, ticket, and show lifecycle events. Eligible notifications may also queue email independently; email failure never changes business success or in-app read state, and delivery diagnostics remain internal. Ticket PDF and password-reset OTP email use separate workflows.")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    @Operation(
            summary = "List my notifications",
            description =
                    "Returns only the authenticated account's in-app notifications, including registration, booking creation/confirmation/cancellation/expiry, ticket issuance, payment success/eligible failure, and show cancellation events. Results use stable occurredAt/id ordering and support pagination and approved filters.")
    public ResponseEntity<ApiResponse<PageResponse<CustomerNotificationSummaryResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) NotificationType type,
            @RequestParam(required = false) NotificationChannel channel,
            @RequestParam(required = false) Boolean read,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime occurredFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime occurredTo) {
        var criteria =
                new NotificationSearchCriteria(type, channel, read, occurredFrom, occurredTo);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Notifications fetched successfully",
                        notificationService.getCustomerNotifications(
                                currentUser(), criteria, page, size, sortDir)));
    }

    @GetMapping("/{notificationId}")
    @Operation(
            summary = "Get my notification",
            description =
                    "Returns an owned in-app notification without marking it read. Unknown and non-owned IDs return 404.")
    public ResponseEntity<ApiResponse<CustomerNotificationDetailResponse>> detail(
            @Parameter(description = "Owned notification ID") @PathVariable Long notificationId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Notification fetched successfully",
                        notificationService.getCustomerNotification(
                                notificationId, currentUser())));
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(
            summary = "Mark my notification read",
            description =
                    "Idempotently marks an owned in-app notification read. Repeated calls preserve the original readAt; non-owned IDs return 404.")
    public ResponseEntity<ApiResponse<CustomerNotificationDetailResponse>> markRead(
            @PathVariable Long notificationId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Notification marked as read",
                        notificationService.markRead(notificationId, currentUser())));
    }

    @PatchMapping("/read-all")
    @Operation(
            summary = "Mark all my notifications read",
            description =
                    "Marks only the authenticated account's unread IN_APP notifications using one timestamp. Already-read and other users' rows are unchanged.")
    public ResponseEntity<ApiResponse<ReadAllNotificationsResponse>> markAllRead() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Notifications marked as read",
                        notificationService.markAllRead(currentUser())));
    }

    @GetMapping("/unread-count")
    @Operation(
            summary = "Count my unread notifications",
            description =
                    "Counts only unread IN_APP notifications owned by the authenticated account.")
    public ResponseEntity<ApiResponse<UnreadNotificationCountResponse>> unreadCount() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Unread notification count fetched successfully",
                        notificationService.unreadCount(currentUser())));
    }

    private User currentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user))
            throw new AuthenticationException("User not authenticated");
        return user;
    }
}
