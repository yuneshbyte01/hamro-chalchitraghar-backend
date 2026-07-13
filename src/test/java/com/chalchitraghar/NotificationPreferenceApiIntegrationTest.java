package com.chalchitraghar;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.chalchitraghar.modules.notifications.enums.NotificationType;
import com.chalchitraghar.modules.notifications.service.*;
import com.chalchitraghar.modules.users.enums.Role;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@TestPropertySource(
        properties = {
            "app.notifications.email.enabled=true",
            "app.notifications.email.from=test@example.com"
        })
class NotificationPreferenceApiIntegrationTest extends AbstractIntegrationTest {
    @MockitoBean private NotificationMailSender mailSender;
    @Autowired private NotificationService notifications;
    @Autowired private NotificationEmailQueueService emailQueue;

    @Test
    void customerListsUpdatesAndEnforcesEmailOnlyPreference() throws Exception {
        var user = saveUser("preference@example.com", Role.CUSTOMER);
        String token = loginToken(user.getEmail());
        mockMvc.perform(
                        get("/api/customer/notification-preferences")
                                .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(10));
        mockMvc.perform(
                        patch("/api/customer/notification-preferences/PAYMENT_SUCCEEDED")
                                .header("Authorization", bearer(token))
                                .contentType("application/json")
                                .content(json(Map.of("enabled", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.source").value("USER_OVERRIDE"));
        var notification =
                notifications.createInAppNotification(
                        user.getId(),
                        NotificationType.PAYMENT_SUCCEEDED,
                        "PAYMENT_SUCCEEDED:901",
                        "Payment successful",
                        "Payment completed.",
                        null,
                        LocalDateTime.now(clock));
        emailQueue.queue(notification);
        org.assertj.core.api.Assertions.assertThat(notificationRepository.count()).isOne();
        org.assertj.core.api.Assertions.assertThat(
                        notificationDeliveryRepository.findAll().getFirst().getFailureReason())
                .isEqualTo("Disabled by user preference");
        mockMvc.perform(
                        patch("/api/customer/notification-preferences/SYSTEM")
                                .header("Authorization", bearer(token))
                                .contentType("application/json")
                                .content(json(Map.of("enabled", false))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/customer/notification-preferences"))
                .andExpect(status().isUnauthorized());
    }
}
