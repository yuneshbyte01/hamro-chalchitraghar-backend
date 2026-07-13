package com.chalchitraghar;

import static org.assertj.core.api.Assertions.assertThat;

import com.chalchitraghar.modules.bookings.entity.Booking;
import com.chalchitraghar.modules.bookings.enums.BookingStatus;
import com.chalchitraghar.modules.halls.enums.Status;
import com.chalchitraghar.modules.movies.enums.MovieStatus;
import com.chalchitraghar.modules.notifications.enums.NotificationType;
import com.chalchitraghar.modules.notifications.service.ShowReminderProcessor;
import com.chalchitraghar.modules.shows.entity.Show;
import com.chalchitraghar.modules.shows.enums.ShowStatus;
import com.chalchitraghar.modules.users.enums.Role;
import java.math.BigDecimal;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(
        properties = {
            "app.notifications.reminders.enabled=true",
            "app.notifications.reminders.show-before=PT2H",
            "app.notifications.reminders.batch-size=10"
        })
class ShowReminderIntegrationTest extends AbstractIntegrationTest {
    @Autowired private ShowReminderProcessor processor;

    @Test
    void confirmedUpcomingBookingGetsOneDeterministicReminder() {
        var movie = saveMovie("Reminder Movie", MovieStatus.NOW_SHOWING);
        var hall = saveHall("Reminder Hall", Status.ACTIVE);
        LocalDateTime showAt = LocalDateTime.now(clock).plusHours(1);
        Show show =
                showRepository.save(
                        Show.builder()
                                .movie(movie)
                                .hall(hall)
                                .status(ShowStatus.SCHEDULED)
                                .showDate(showAt.toLocalDate())
                                .showTime(showAt.toLocalTime())
                                .endTime(showAt.toLocalTime().plusHours(2))
                                .build());
        var user = saveUser("reminder-owner@example.com", Role.CUSTOMER);
        Booking confirmed =
                bookingRepository.save(
                        Booking.builder()
                                .bookingReference("BK-REMINDER-1")
                                .user(user)
                                .show(show)
                                .bookingTime(LocalDateTime.now(clock))
                                .status(BookingStatus.CONFIRMED)
                                .totalAmount(BigDecimal.TEN)
                                .currency("NPR")
                                .confirmedAt(LocalDateTime.now(clock))
                                .build());

        assertThat(processor.processBatch()).isOne();
        assertThat(processor.processBatch()).isZero();
        assertThat(notificationRepository.findAll())
                .singleElement()
                .satisfies(
                        n -> {
                            assertThat(n.getType()).isEqualTo(NotificationType.SHOW_REMINDER);
                            assertThat(n.getEventKey())
                                    .isEqualTo("SHOW_REMINDER:" + confirmed.getId() + ":PT2H");
                            assertThat(n.getMessage())
                                    .contains("BK-REMINDER-1", "Reminder Hall")
                                    .doesNotContain("QR");
                        });
        assertThat(notificationDeliveryRepository.findAll())
                .singleElement()
                .satisfies(
                        d -> assertThat(d.getFailureReason()).isEqualTo("Email delivery disabled"));
    }

    @Test
    void initiatedAndCancelledShowsDoNotReceiveReminders() {
        var movie = saveMovie("Ineligible Reminder", MovieStatus.NOW_SHOWING);
        var hall = saveHall("Ineligible Hall", Status.ACTIVE);
        LocalDateTime at = LocalDateTime.now(clock).plusHours(1);
        Show show =
                showRepository.save(
                        Show.builder()
                                .movie(movie)
                                .hall(hall)
                                .status(ShowStatus.SCHEDULED)
                                .showDate(at.toLocalDate())
                                .showTime(at.toLocalTime())
                                .endTime(at.toLocalTime().plusHours(2))
                                .build());
        show.setStatus(ShowStatus.CANCELLED);
        showRepository.save(show);
        var user = saveUser("no-reminder@example.com", Role.CUSTOMER);
        bookingRepository.save(
                Booking.builder()
                        .bookingReference("BK-NO-REMINDER")
                        .user(user)
                        .show(show)
                        .bookingTime(LocalDateTime.now(clock))
                        .status(BookingStatus.INITIATED)
                        .totalAmount(BigDecimal.TEN)
                        .currency("NPR")
                        .build());
        assertThat(processor.processBatch()).isZero();
        assertThat(notificationRepository.count()).isZero();
    }
}
