package com.chalchitraghar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chalchitraghar.modules.notifications.entity.Notification;
import com.chalchitraghar.modules.notifications.enums.NotificationChannel;
import com.chalchitraghar.modules.notifications.enums.NotificationType;
import com.chalchitraghar.modules.notifications.service.NotificationService;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.modules.users.enums.Role;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class NotificationApiIntegrationTest extends AbstractIntegrationTest {
    @Autowired private NotificationService notificationService;

    @Test
    void creationPersistsSafeDefaultsPayloadAndInjectedClockTime() {
        User user = saveUser("notification-create@example.com", Role.CUSTOMER);
        LocalDateTime before = LocalDateTime.now(clock);

        Notification created =
                create(
                        user,
                        "SYSTEM:CREATE",
                        NotificationType.SYSTEM,
                        "  System title  ",
                        "  System message  ",
                        "{\"bookingReference\":\"BK-1\"}",
                        null);

        LocalDateTime after = LocalDateTime.now(clock);
        assertThat(created.getChannel()).isEqualTo(NotificationChannel.IN_APP);
        assertThat(created.getType()).isEqualTo(NotificationType.SYSTEM);
        assertThat(created.getTitle()).isEqualTo("System title");
        assertThat(created.getMessage()).isEqualTo("System message");
        assertThat(created.getPayload()).isEqualTo("{\"bookingReference\":\"BK-1\"}");
        assertThat(created.isRead()).isFalse();
        assertThat(created.getReadAt()).isNull();
        assertThat(created.getOccurredAt()).isBetween(before, after);
    }

    @Test
    void creationValidatesRequiredFieldsLengthsAndJson() {
        User user = saveUser("notification-validation@example.com", Role.CUSTOMER);

        assertThatThrownBy(
                        () ->
                                notificationService.createInAppNotification(
                                        user.getId(), null, "key", "title", "message", null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () ->
                                notificationService.createInAppNotification(
                                        user.getId(),
                                        NotificationType.SYSTEM,
                                        " ",
                                        "title",
                                        "message",
                                        null,
                                        null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () ->
                                notificationService.createInAppNotification(
                                        user.getId(),
                                        NotificationType.SYSTEM,
                                        "key",
                                        " ",
                                        "message",
                                        null,
                                        null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () ->
                                notificationService.createInAppNotification(
                                        user.getId(),
                                        NotificationType.SYSTEM,
                                        "key",
                                        "title",
                                        " ",
                                        null,
                                        null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () ->
                                notificationService.createInAppNotification(
                                        user.getId(),
                                        NotificationType.SYSTEM,
                                        "key",
                                        "title",
                                        "message",
                                        "not-json",
                                        null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void creationIsIdempotentByUserEventAndChannel() {
        User firstUser = saveUser("notification-idempotent@example.com", Role.CUSTOMER);
        User secondUser = saveUser("notification-other@example.com", Role.CUSTOMER);

        Notification first =
                create(
                        firstUser,
                        "SHARED:1",
                        NotificationType.WELCOME,
                        "First",
                        "First",
                        null,
                        null);
        Notification repeated =
                create(
                        firstUser,
                        "SHARED:1",
                        NotificationType.SYSTEM,
                        "Changed",
                        "Changed",
                        null,
                        null);
        Notification otherUser =
                create(
                        secondUser,
                        "SHARED:1",
                        NotificationType.WELCOME,
                        "Other",
                        "Other",
                        null,
                        null);
        Notification otherEvent =
                create(
                        firstUser,
                        "SHARED:2",
                        NotificationType.WELCOME,
                        "Second",
                        "Second",
                        null,
                        null);

        assertThat(repeated.getId()).isEqualTo(first.getId());
        assertThat(repeated.getTitle()).isEqualTo("First");
        assertThat(otherUser.getId()).isNotEqualTo(first.getId());
        assertThat(otherEvent.getId()).isNotEqualTo(first.getId());
        assertThat(notificationRepository.count()).isEqualTo(3);
    }

    @Test
    void concurrentCreationProducesOneRow() throws Exception {
        User user = saveUser("notification-concurrent@example.com", Role.CUSTOMER);
        Callable<Long> task =
                () ->
                        create(
                                        user,
                                        "CONCURRENT:1",
                                        NotificationType.SYSTEM,
                                        "Concurrent",
                                        "Concurrent",
                                        null,
                                        null)
                                .getId();
        try (var executor = Executors.newFixedThreadPool(4)) {
            List<Callable<Long>> tasks = List.of(task, task, task, task);
            var ids =
                    executor.invokeAll(tasks).stream()
                            .map(
                                    f -> {
                                        try {
                                            return f.get();
                                        } catch (Exception exception) {
                                            throw new RuntimeException(exception);
                                        }
                                    })
                            .toList();
            assertThat(ids).containsOnly(ids.getFirst());
        }
        assertThat(notificationRepository.count()).isOne();
    }

    @Test
    void databaseUniqueAndRequiredConstraintsAreEnforced() {
        User user = saveUser("notification-constraints@example.com", Role.CUSTOMER);
        create(user, "UNIQUE:1", NotificationType.SYSTEM, "Title", "Message", null, null);
        Notification duplicate =
                Notification.builder()
                        .user(user)
                        .type(NotificationType.SYSTEM)
                        .channel(NotificationChannel.IN_APP)
                        .eventKey("UNIQUE:1")
                        .title("Title")
                        .message("Message")
                        .occurredAt(LocalDateTime.now(clock))
                        .build();
        duplicate.setCreatedAt(LocalDateTime.now(clock));
        duplicate.setUpdatedAt(LocalDateTime.now(clock));

        assertThatThrownBy(() -> notificationRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void customerListIsOwnerScopedStablePaginatedAndFilterable() throws Exception {
        User owner = saveUser("notification-list@example.com", Role.CUSTOMER);
        String token = loginToken(owner.getEmail());
        User other = saveUser("notification-list-other@example.com", Role.CUSTOMER);
        LocalDateTime time = LocalDateTime.now(clock).minusHours(1);
        Notification older =
                create(
                        owner,
                        "LIST:1",
                        NotificationType.SYSTEM,
                        "Older",
                        "Older message",
                        null,
                        time);
        Notification tiedFirst =
                create(
                        owner,
                        "LIST:2",
                        NotificationType.BOOKING_CONFIRMED,
                        "Tie one",
                        "Tie",
                        null,
                        time.plusMinutes(1));
        Notification tiedSecond =
                create(
                        owner,
                        "LIST:3",
                        NotificationType.BOOKING_CONFIRMED,
                        "Tie two",
                        "Tie",
                        null,
                        time.plusMinutes(1));
        tiedFirst.setRead(true);
        tiedFirst.setReadAt(LocalDateTime.now(clock));
        notificationRepository.save(tiedFirst);
        create(
                other,
                "LIST:4",
                NotificationType.SYSTEM,
                "Hidden",
                "Hidden",
                null,
                time.plusHours(2));

        mockMvc.perform(get("/api/customer/notifications").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.content[0].id").value(tiedSecond.getId()))
                .andExpect(jsonPath("$.data.content[1].id").value(tiedFirst.getId()))
                .andExpect(jsonPath("$.data.content[2].id").value(older.getId()));

        mockMvc.perform(
                        get("/api/customer/notifications")
                                .header("Authorization", bearer(token))
                                .param("page", "0")
                                .param("size", "1")
                                .param("type", "BOOKING_CONFIRMED")
                                .param("read", "false")
                                .param("channel", "IN_APP")
                                .param("occurredFrom", time.toString())
                                .param("occurredTo", time.plusHours(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(tiedSecond.getId()));
    }

    @Test
    void listValidatesFiltersPaginationAndAuthentication() throws Exception {
        String token = tokenFor("notification-filter-errors@example.com", Role.CUSTOMER);

        mockMvc.perform(get("/api/customer/notifications")).andExpect(status().isUnauthorized());
        mockMvc.perform(
                        get("/api/customer/notifications")
                                .header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(
                        get("/api/customer/notifications")
                                .header("Authorization", bearer(token))
                                .param("type", "INVALID"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(
                        get("/api/customer/notifications")
                                .header("Authorization", bearer(token))
                                .param("occurredFrom", "invalid"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(
                        get("/api/customer/notifications")
                                .header("Authorization", bearer(token))
                                .param("occurredFrom", "2026-08-02T00:00:00")
                                .param("occurredTo", "2026-08-01T00:00:00"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(
                        get("/api/customer/notifications")
                                .header("Authorization", bearer(token))
                                .param("size", "101"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(
                        get("/api/customer/notifications")
                                .header("Authorization", bearer(token))
                                .param("channel", "EMAIL"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void detailIsOwnerOnlySafeAndDoesNotMarkRead() throws Exception {
        User owner = saveUser("notification-detail@example.com", Role.CUSTOMER);
        String ownerToken = loginToken(owner.getEmail());
        String otherToken = tokenFor("notification-detail-other@example.com", Role.CUSTOMER);
        Notification notification =
                create(
                        owner,
                        "DETAIL:SECRET-EVENT-KEY",
                        NotificationType.SYSTEM,
                        "Safe title",
                        "Safe message",
                        "{\"bookingReference\":\"BK-SAFE\"}",
                        null);

        mockMvc.perform(
                        get("/api/customer/notifications/{id}", notification.getId())
                                .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Safe title"))
                .andExpect(jsonPath("$.data.payload.bookingReference").value("BK-SAFE"))
                .andExpect(jsonPath("$.data.read").value(false))
                .andExpect(jsonPath("$.data.user").doesNotExist())
                .andExpect(jsonPath("$.data.userId").doesNotExist())
                .andExpect(jsonPath("$.data.eventKey").doesNotExist())
                .andExpect(jsonPath("$.data.deliveryStatus").doesNotExist());
        assertThat(notificationRepository.findById(notification.getId()).orElseThrow().isRead())
                .isFalse();

        mockMvc.perform(
                        get("/api/customer/notifications/{id}", notification.getId())
                                .header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                        get("/api/customer/notifications/{id}", Long.MAX_VALUE)
                                .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void markReadIsOwnerOnlyIdempotentAndUsesClock() throws Exception {
        User owner = saveUser("notification-read@example.com", Role.CUSTOMER);
        String token = loginToken(owner.getEmail());
        String otherToken = tokenFor("notification-read-other@example.com", Role.CUSTOMER);
        Notification notification =
                create(owner, "READ:1", NotificationType.SYSTEM, "Read", "Read", null, null);
        LocalDateTime before = LocalDateTime.now(clock);

        mockMvc.perform(
                        patch("/api/customer/notifications/{id}/read", notification.getId())
                                .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.read").value(true));
        Notification firstRead =
                notificationRepository.findById(notification.getId()).orElseThrow();
        assertThat(firstRead.getReadAt()).isAfterOrEqualTo(before);
        LocalDateTime originalReadAt = firstRead.getReadAt();

        mockMvc.perform(
                        patch("/api/customer/notifications/{id}/read", notification.getId())
                                .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        assertThat(notificationRepository.findById(notification.getId()).orElseThrow().getReadAt())
                .isEqualTo(originalReadAt);

        mockMvc.perform(
                        patch("/api/customer/notifications/{id}/read", notification.getId())
                                .header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void readAllAndUnreadCountAreOwnerAndChannelScoped() throws Exception {
        User owner = saveUser("notification-read-all@example.com", Role.CUSTOMER);
        String token = loginToken(owner.getEmail());
        User other = saveUser("notification-read-all-other@example.com", Role.CUSTOMER);
        Notification first =
                create(owner, "ALL:1", NotificationType.SYSTEM, "One", "One", null, null);
        Notification second =
                create(owner, "ALL:2", NotificationType.SYSTEM, "Two", "Two", null, null);
        Notification alreadyRead =
                create(owner, "ALL:3", NotificationType.SYSTEM, "Three", "Three", null, null);
        LocalDateTime preserved =
                LocalDateTime.now(clock).minusHours(2).truncatedTo(ChronoUnit.MICROS);
        alreadyRead.setRead(true);
        alreadyRead.setReadAt(preserved);
        notificationRepository.save(alreadyRead);
        Notification otherNotification =
                create(other, "ALL:4", NotificationType.SYSTEM, "Other", "Other", null, null);

        mockMvc.perform(
                        get("/api/customer/notifications/unread-count")
                                .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(2));
        mockMvc.perform(
                        patch("/api/customer/notifications/read-all")
                                .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updatedCount").value(2));

        Notification updatedFirst = notificationRepository.findById(first.getId()).orElseThrow();
        Notification updatedSecond = notificationRepository.findById(second.getId()).orElseThrow();
        assertThat(updatedFirst.getReadAt()).isEqualTo(updatedSecond.getReadAt());
        assertThat(notificationRepository.findById(alreadyRead.getId()).orElseThrow().getReadAt())
                .isEqualTo(preserved);
        assertThat(
                        notificationRepository
                                .findById(otherNotification.getId())
                                .orElseThrow()
                                .isRead())
                .isFalse();

        mockMvc.perform(
                        patch("/api/customer/notifications/read-all")
                                .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updatedCount").value(0));
        mockMvc.perform(
                        get("/api/customer/notifications/unread-count")
                                .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(0));
    }

    @Test
    void staffAndAdminCustomerRoutesRemainBoundToTheirOwnAccounts() throws Exception {
        User staff = saveUser("notification-staff@example.com", Role.STAFF);
        String staffToken = loginToken(staff.getEmail());
        User admin = saveUser("notification-admin@example.com", Role.ADMIN);
        String adminToken = loginToken(admin.getEmail());
        create(staff, "ROLE:STAFF", NotificationType.SYSTEM, "Staff", "Staff", null, null);
        create(admin, "ROLE:ADMIN", NotificationType.SYSTEM, "Admin", "Admin", null, null);

        mockMvc.perform(
                        get("/api/customer/notifications")
                                .header("Authorization", bearer(staffToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Staff"));
        mockMvc.perform(
                        get("/api/customer/notifications")
                                .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Admin"));
    }

    private Notification create(
            User user,
            String eventKey,
            NotificationType type,
            String title,
            String message,
            String payload,
            LocalDateTime occurredAt) {
        return notificationService.createInAppNotification(
                user.getId(), type, eventKey, title, message, payload, occurredAt);
    }
}
