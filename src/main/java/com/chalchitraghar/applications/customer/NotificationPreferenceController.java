package com.chalchitraghar.applications.customer;

import com.chalchitraghar.modules.notifications.dto.request.UpdateNotificationPreferenceRequest;
import com.chalchitraghar.modules.notifications.dto.response.CustomerNotificationPreferenceResponse;
import com.chalchitraghar.modules.notifications.enums.NotificationType;
import com.chalchitraghar.modules.notifications.service.NotificationPreferenceService;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.shared.exception.AuthenticationException;
import com.chalchitraghar.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/notification-preferences")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(
        name = "Customer Notification Preferences",
        description = "EMAIL-only preferences; in-app and security-specific email remain enabled")
public class NotificationPreferenceController {
    private final NotificationPreferenceService service;

    @GetMapping
    @Operation(summary = "List effective email notification preferences")
    public ResponseEntity<ApiResponse<List<CustomerNotificationPreferenceResponse>>> list() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Notification preferences fetched", service.list(currentUser())));
    }

    @PatchMapping("/{type}")
    @Operation(
            summary = "Set an email notification preference",
            description =
                    "Affects future general email queueing only; existing deliveries are unchanged")
    public ResponseEntity<ApiResponse<CustomerNotificationPreferenceResponse>> update(
            @PathVariable NotificationType type,
            @Valid @RequestBody UpdateNotificationPreferenceRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Notification preference updated",
                        service.update(currentUser(), type, request.enabled())));
    }

    private User currentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User user) return user;
        throw new AuthenticationException("Authentication is required");
    }
}
