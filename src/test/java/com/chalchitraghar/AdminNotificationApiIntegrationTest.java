package com.chalchitraghar;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.chalchitraghar.modules.notifications.enums.NotificationType;
import com.chalchitraghar.modules.notifications.service.*;
import com.chalchitraghar.modules.users.enums.Role;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AdminNotificationApiIntegrationTest extends AbstractIntegrationTest {
    @Autowired private NotificationService notifications;
    @Autowired private NotificationEmailQueueService queue;

    @Test
    void adminCanInspectMaskedNotificationsWhileOtherRolesAreForbidden() throws Exception {
        var owner = saveUser("admin-notification-owner@example.com", Role.CUSTOMER);
        var notification =
                notifications.createInAppNotification(
                        owner.getId(),
                        NotificationType.WELCOME,
                        "USER_REGISTERED:" + owner.getId(),
                        "Welcome",
                        "Welcome.",
                        null,
                        LocalDateTime.now(clock));
        var delivery = queue.queue(notification);
        String admin = tokenFor("notification-admin@example.com", Role.ADMIN);
        String customer = loginToken(owner.getEmail());
        String staff = tokenFor("notification-staff@example.com", Role.STAFF);

        mockMvc.perform(
                        get("/api/admin/notifications")
                                .header("Authorization", bearer(admin))
                                .param("type", "WELCOME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].maskedEmail").value("ad***@example.com"));
        mockMvc.perform(
                        get("/api/admin/notifications/{id}", notification.getId())
                                .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eventKey").exists())
                .andExpect(
                        jsonPath("$.data.deliveries[0].maskedRecipient").value("ad***@example.com"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
        mockMvc.perform(
                        get("/api/admin/notification-deliveries/{id}", delivery.getId())
                                .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.maskedRecipient").value("ad***@example.com"));
        mockMvc.perform(get("/api/admin/notifications").header("Authorization", bearer(customer)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/notifications").header("Authorization", bearer(staff)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/notifications")).andExpect(status().isUnauthorized());
        mockMvc.perform(
                        post("/api/admin/notification-deliveries/{id}/retry", delivery.getId())
                                .header("Authorization", bearer(admin)))
                .andExpect(status().isConflict());
    }
}
