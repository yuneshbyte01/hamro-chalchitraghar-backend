package com.chalchitraghar.modules.notifications.service.impl;

import com.chalchitraghar.modules.bookings.repository.BookingRepository;
import com.chalchitraghar.modules.notifications.config.NotificationReminderProperties;
import com.chalchitraghar.modules.notifications.enums.NotificationChannel;
import com.chalchitraghar.modules.notifications.event.ShowReminderDueEvent;
import com.chalchitraghar.modules.notifications.repository.NotificationRepository;
import com.chalchitraghar.modules.notifications.service.ShowReminderProcessor;
import java.time.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShowReminderProcessorImpl implements ShowReminderProcessor {
    private final BookingRepository bookings;
    private final NotificationRepository notifications;
    private final NotificationReminderProperties properties;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Override
    @Transactional
    public int processBatch() {
        if (!properties.isEnabled()) return 0;
        ZonedDateTime now = ZonedDateTime.now(clock);
        ZonedDateTime limit = now.plus(properties.getShowBefore());
        String window = properties.getShowBefore().toString();
        int created = 0;
        for (var booking :
                bookings.findReminderCandidates(
                        now.toLocalDate(),
                        now.toLocalTime(),
                        limit.toLocalDate(),
                        limit.toLocalTime(),
                        PageRequest.of(0, properties.getBatchSize()))) {
            String key = "SHOW_REMINDER:" + booking.getId() + ":" + window;
            if (notifications.existsByUserIdAndEventKeyAndChannel(
                    booking.getUser().getId(), key, NotificationChannel.IN_APP)) continue;
            var show = booking.getShow();
            events.publishEvent(
                    new ShowReminderDueEvent(
                            booking.getUser().getId(),
                            booking.getId(),
                            booking.getBookingReference(),
                            show.getId(),
                            show.getMovie().getTitle(),
                            show.getHall().getName(),
                            LocalDateTime.of(show.getShowDate(), show.getShowTime()),
                            window,
                            now.toLocalDateTime()));
            created++;
        }
        log.info("Processed show reminders created={} window={}", created, window);
        return created;
    }
}
