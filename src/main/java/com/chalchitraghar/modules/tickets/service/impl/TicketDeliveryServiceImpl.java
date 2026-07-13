package com.chalchitraghar.modules.tickets.service.impl;

import com.chalchitraghar.modules.bookings.repository.BookingRepository;
import com.chalchitraghar.modules.tickets.config.TicketOperationsProperties;
import com.chalchitraghar.modules.tickets.entity.*;
import com.chalchitraghar.modules.tickets.enums.*;
import com.chalchitraghar.modules.tickets.repository.TicketDeliveryRepository;
import com.chalchitraghar.modules.tickets.service.*;
import jakarta.mail.util.ByteArrayDataSource;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.javamail.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
public class TicketDeliveryServiceImpl implements TicketDeliveryService {
    private final TicketDeliveryRepository deliveries;
    private final BookingRepository bookings;
    private final TicketPdfService pdf;
    private final JavaMailSender sender;
    private final TicketOperationsProperties props;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void queueAndAttempt(Long bookingId) {
        var b = bookings.findById(bookingId).orElseThrow();
        var d =
                deliveries
                        .findByBookingIdAndChannel(bookingId, TicketDeliveryChannel.EMAIL)
                        .orElseGet(
                                () ->
                                        deliveries.save(
                                                TicketDelivery.builder()
                                                        .booking(b)
                                                        .channel(TicketDeliveryChannel.EMAIL)
                                                        .recipient(b.getUser().getEmail())
                                                        .status(TicketDeliveryStatus.PENDING)
                                                        .attemptCount(0)
                                                        .build()));
        if (props.email().enabled() && d.getStatus() != TicketDeliveryStatus.SENT)
            attempt(d.getId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void attempt(Long id) {
        TicketDelivery d = deliveries.findById(id).orElseThrow();
        if (d.getStatus() == TicketDeliveryStatus.SENT
                || d.getAttemptCount() >= props.email().maxAttempts()) return;
        LocalDateTime now = LocalDateTime.now(clock);
        d.setAttemptCount(d.getAttemptCount() + 1);
        d.setLastAttemptAt(now);
        try {
            var b = d.getBooking();
            MimeMessageHelper m = new MimeMessageHelper(sender.createMimeMessage(), true);
            m.setFrom(props.email().from());
            m.setTo(d.getRecipient());
            m.setSubject("Your Hamro Chalchitraghar Tickets - " + b.getBookingReference());
            m.setText(
                    "Hello "
                            + b.getUser().getName()
                            + ",\n\nYour tickets for "
                            + b.getShow().getMovie().getTitle()
                            + " are attached. Booking: "
                            + b.getBookingReference()
                            + ". Each QR is single-use.\n\nHamro Chalchitraghar");
            m.addAttachment(
                    "tickets-" + b.getBookingReference() + ".pdf",
                    new ByteArrayDataSource(pdf.bookingPdf(b.getId()), "application/pdf"));
            sender.send(m.getMimeMessage());
            d.setStatus(TicketDeliveryStatus.SENT);
            d.setSentAt(now);
            d.setFailureMessage(null);
        } catch (Exception e) {
            d.setStatus(TicketDeliveryStatus.FAILED);
            d.setFailureMessage("Ticket email delivery failed");
        }
        deliveries.save(d);
    }

    @Transactional
    public int retryBatch() {
        if (!props.email().enabled()) return 0;
        var list =
                deliveries.findByStatusInAndAttemptCountLessThanOrderByCreatedAtAsc(
                        List.of(TicketDeliveryStatus.PENDING, TicketDeliveryStatus.FAILED),
                        props.email().maxAttempts(),
                        PageRequest.of(0, props.email().retryBatchSize()));
        list.forEach(d -> attempt(d.getId()));
        return list.size();
    }
}
