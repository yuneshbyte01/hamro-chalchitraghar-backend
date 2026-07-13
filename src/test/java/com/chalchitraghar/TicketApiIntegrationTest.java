package com.chalchitraghar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.chalchitraghar.modules.bookings.entity.Booking;
import com.chalchitraghar.modules.bookings.entity.BookingSeat;
import com.chalchitraghar.modules.bookings.enums.BookingStatus;
import com.chalchitraghar.modules.seats.enums.SeatStatus;
import com.chalchitraghar.modules.tickets.dto.request.TicketScanRequest;
import com.chalchitraghar.modules.tickets.enums.TicketStatus;
import com.chalchitraghar.modules.tickets.enums.ValidationResult;
import com.chalchitraghar.modules.tickets.service.ExpiredTicketCleanupJob;
import com.chalchitraghar.modules.tickets.service.QrTokenService;
import com.chalchitraghar.modules.tickets.service.TicketIssuanceService;
import com.chalchitraghar.modules.tickets.service.TicketOperationsService;
import com.chalchitraghar.modules.tickets.service.TicketValidationService;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.modules.users.enums.Role;
import com.chalchitraghar.shared.exception.InvalidBookingStateException;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TicketApiIntegrationTest extends AbstractIntegrationTest {
    @Autowired TicketIssuanceService issuance;
    @Autowired QrTokenService qrTokens;
    @Autowired TicketValidationService validationService;
    @Autowired TicketOperationsService operations;
    @Autowired ExpiredTicketCleanupJob expiryJob;

    @Test
    void confirmedTwoSeatBookingIssuesExactlyOneTicketPerSeatIdempotently() throws Exception {
        Context c = context("ticket-owner@example.com", 2, BookingStatus.CONFIRMED);
        LocalDateTime beforeIssuance = LocalDateTime.now(clock);
        var first = issuance.issueTicketsForConfirmedBooking(c.booking());
        LocalDateTime afterIssuance = LocalDateTime.now(clock);
        var second = issuance.issueTicketsForConfirmedBooking(c.booking());

        assertThat(first).hasSize(2);
        assertThat(second)
                .extracting(t -> t.getId())
                .containsExactlyElementsOf(first.stream().map(t -> t.getId()).toList());
        assertThat(ticketRepository.count()).isEqualTo(2);
        assertThat(first)
                .extracting(t -> t.getBookingSeat().getSeat().getPositionIndex())
                .isSorted();
        assertThat(first)
                .allSatisfy(
                        t -> {
                            assertThat(t.getStatus()).isEqualTo(TicketStatus.ISSUED);
                            assertThat(t.getIssuedAt()).isBetween(beforeIssuance, afterIssuance);
                            assertThat(t.getTicketReference()).matches("TKT-\\d{8}-[A-Z0-9]{8}");
                            assertThat(t.getBookingSeat().getSeat().getSeatStatus())
                                    .isEqualTo(SeatStatus.BOOKED);
                        });
    }

    @Test
    void nonConfirmedBookingsAreRejectedAndIssueNothing() throws Exception {
        Context c = context("initiated-ticket@example.com", 1, BookingStatus.INITIATED);
        assertThatThrownBy(() -> issuance.issueTicketsForConfirmedBooking(c.booking()))
                .isInstanceOf(InvalidBookingStateException.class);
        assertThat(ticketRepository.count()).isZero();
    }

    @Test
    void customerReadsOnlyOwnedTicketsAndResponsesAreSafe() throws Exception {
        Context owner = context("ticket-api-owner@example.com", 2, BookingStatus.CONFIRMED);
        Context other = context("ticket-api-other@example.com", 1, BookingStatus.CONFIRMED);
        var owned = issuance.issueTicketsForConfirmedBooking(owner.booking());
        var foreign = issuance.issueTicketsForConfirmedBooking(other.booking()).getFirst();

        mockMvc.perform(get("/api/customer/tickets").header("Authorization", bearer(owner.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].customerId").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].qrTokenVersion").doesNotExist());
        mockMvc.perform(
                        get("/api/customer/tickets/{ref}", owned.getFirst().getTicketReference())
                                .header("Authorization", bearer(owner.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.seatCode").exists())
                .andExpect(jsonPath("$.data.customerEmail").doesNotExist())
                .andExpect(jsonPath("$.data.id").doesNotExist());
        mockMvc.perform(
                        get("/api/customer/tickets/{ref}", foreign.getTicketReference())
                                .header("Authorization", bearer(owner.token())))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                        get(
                                        "/api/customer/bookings/{ref}/tickets",
                                        owner.booking().getBookingReference())
                                .header("Authorization", bearer(owner.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void staffAndAdminRoutesEnforceAudienceAndExposeOperationalIdentity() throws Exception {
        Context c = context("ticket-roles-owner@example.com", 1, BookingStatus.CONFIRMED);
        var ticket = issuance.issueTicketsForConfirmedBooking(c.booking()).getFirst();
        String staff = tokenFor("ticket-staff@example.com", Role.STAFF);
        String admin = tokenFor("ticket-admin@example.com", Role.ADMIN);

        mockMvc.perform(
                        get("/api/staff/tickets/{ref}", ticket.getTicketReference())
                                .header("Authorization", bearer(staff)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customerEmail").value(c.user().getEmail()));
        mockMvc.perform(
                        get("/api/staff/bookings/{ref}/tickets", c.booking().getBookingReference())
                                .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
        mockMvc.perform(
                        get("/api/admin/tickets/{ref}", ticket.getTicketReference())
                                .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
        mockMvc.perform(
                        get("/api/staff/tickets/{ref}", ticket.getTicketReference())
                                .header("Authorization", bearer(c.token())))
                .andExpect(status().isForbidden());
        mockMvc.perform(
                        get("/api/admin/tickets/{ref}", ticket.getTicketReference())
                                .header("Authorization", bearer(staff)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/customer/tickets")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/customer/tickets").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ownerRetrievesDeterministicOpaqueQrPngAndSafeMetadata() throws Exception {
        Context owner = context("qr-owner@example.com", 1, BookingStatus.CONFIRMED);
        Context other = context("qr-other@example.com", 1, BookingStatus.CONFIRMED);
        var ticket = issuance.issueTicketsForConfirmedBooking(owner.booking()).getFirst();
        String raw = qrTokens.decryptToken(ticket);
        assertThat(ticket.getQrTokenEncrypted()).isNotEqualTo(raw);
        assertThat(ticket.getQrTokenHash()).isNotEqualTo(raw);
        assertThat(qrTokens.matches(raw, ticket.getQrTokenHash())).isTrue();
        assertThat(ticketRepository.findByQrTokenHash(ticket.getQrTokenHash())).isPresent();

        byte[] first =
                mockMvc.perform(
                                get("/api/customer/tickets/{ref}/qr", ticket.getTicketReference())
                                        .header("Authorization", bearer(owner.token())))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType("image/png"))
                        .andExpect(header().string("Cache-Control", "private, no-store"))
                        .andReturn()
                        .getResponse()
                        .getContentAsByteArray();
        byte[] second =
                mockMvc.perform(
                                get("/api/customer/tickets/{ref}/qr", ticket.getTicketReference())
                                        .header("Authorization", bearer(owner.token())))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsByteArray();
        assertThat(first).isEqualTo(second);
        var image = ImageIO.read(new ByteArrayInputStream(first));
        assertThat(image).isNotNull();
        assertThat(image.getWidth()).isEqualTo(400);
        assertThat(image.getHeight()).isEqualTo(400);
        String decoded =
                new MultiFormatReader()
                        .decode(
                                new BinaryBitmap(
                                        new HybridBinarizer(
                                                new BufferedImageLuminanceSource(image))))
                        .getText();
        assertThat(decoded)
                .isEqualTo(raw)
                .doesNotContain(
                        ticket.getTicketReference(), ticket.getBooking().getBookingReference());

        mockMvc.perform(
                        get("/api/customer/tickets/{ref}/qr-data", ticket.getTicketReference())
                                .header("Authorization", bearer(owner.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.qrVersion").value(1))
                .andExpect(jsonPath("$.data.qrTokenHash").doesNotExist())
                .andExpect(jsonPath("$.data.qrTokenEncrypted").doesNotExist());
        mockMvc.perform(
                        get("/api/customer/tickets/{ref}/qr", ticket.getTicketReference())
                                .header("Authorization", bearer(other.token())))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/customer/tickets/{ref}/qr", ticket.getTicketReference()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void staffScanChecksInAtomicallyPreventsReplayAndCreatesHistory() throws Exception {
        Context c = context("scan-owner@example.com", 1, BookingStatus.CONFIRMED);
        makeCurrent(c.booking());
        var ticket = issuance.issueTicketsForConfirmedBooking(c.booking()).getFirst();
        String raw = qrTokens.decryptToken(ticket);
        String staffToken = tokenFor("scanner@example.com", Role.STAFF);
        String body = json(java.util.Map.of("qrToken", raw));
        mockMvc.perform(
                        post("/api/staff/tickets/scan")
                                .header("Authorization", bearer(staffToken))
                                .header("X-Device-ID", "gate-1")
                                .header("X-Location", "main entrance")
                                .header("X-Request-ID", "scan-1")
                                .contentType("application/json")
                                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.admitted").value(true));
        mockMvc.perform(
                        post("/api/staff/tickets/scan")
                                .header("Authorization", bearer(staffToken))
                                .contentType("application/json")
                                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.result").value("ALREADY_USED"))
                .andExpect(jsonPath("$.data.admitted").value(false));
        var checked = ticketRepository.findById(ticket.getId()).orElseThrow();
        assertThat(checked.getStatus()).isEqualTo(TicketStatus.CHECKED_IN);
        assertThat(checked.getCheckedInAt()).isNotNull();
        assertThat(checked.getCheckedInBy().getId())
                .isEqualTo(userRepository.findByEmail("scanner@example.com").orElseThrow().getId());
        assertThat(
                        ticketValidationRepository.findByTicketIdOrderByValidationTimeDesc(
                                ticket.getId()))
                .extracting(v -> v.getResult())
                .containsExactly(ValidationResult.ALREADY_USED, ValidationResult.SUCCESS);
        mockMvc.perform(
                        get("/api/staff/tickets/{ref}", ticket.getTicketReference())
                                .header("Authorization", bearer(staffToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.validationHistory.length()").value(2));
        mockMvc.perform(
                        get("/api/customer/tickets/{ref}", ticket.getTicketReference())
                                .header("Authorization", bearer(c.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checkedIn").value(true))
                .andExpect(jsonPath("$.data.checkedInAt").exists())
                .andExpect(jsonPath("$.data.checkedInBy").doesNotExist());
    }

    @Test
    void scanRejectsLifecycleTimingAuthorizationAndUnknownTokens() throws Exception {
        String staff = tokenFor("rules-scanner@example.com", Role.STAFF);
        Context early = context("early@example.com", 1, BookingStatus.CONFIRMED);
        var earlyTicket = issuance.issueTicketsForConfirmedBooking(early.booking()).getFirst();
        scan(staff, qrTokens.decryptToken(earlyTicket))
                .andExpect(jsonPath("$.data.result").value("TOO_EARLY"));
        Context cancelled = context("cancelled-show@example.com", 1, BookingStatus.CONFIRMED);
        cancelled
                .booking()
                .getShow()
                .setStatus(com.chalchitraghar.modules.shows.enums.ShowStatus.CANCELLED);
        showRepository.save(cancelled.booking().getShow());
        var cancelledTicket =
                issuance.issueTicketsForConfirmedBooking(cancelled.booking()).getFirst();
        scan(staff, qrTokens.decryptToken(cancelledTicket))
                .andExpect(jsonPath("$.data.result").value("SHOW_CANCELLED"));
        Context revoked = context("revoked@example.com", 1, BookingStatus.CONFIRMED);
        makeCurrent(revoked.booking());
        var revokedTicket = issuance.issueTicketsForConfirmedBooking(revoked.booking()).getFirst();
        revokedTicket.setStatus(TicketStatus.REVOKED);
        ticketRepository.save(revokedTicket);
        scan(staff, qrTokens.decryptToken(revokedTicket))
                .andExpect(jsonPath("$.data.result").value("REVOKED"));
        Context expired = context("expired-ticket@example.com", 1, BookingStatus.CONFIRMED);
        makeCurrent(expired.booking());
        var expiredTicket = issuance.issueTicketsForConfirmedBooking(expired.booking()).getFirst();
        expiredTicket.setStatus(TicketStatus.EXPIRED);
        ticketRepository.save(expiredTicket);
        scan(staff, qrTokens.decryptToken(expiredTicket))
                .andExpect(jsonPath("$.data.result").value("EXPIRED"));
        Context bookingCancelled =
                context("booking-cancelled@example.com", 1, BookingStatus.CONFIRMED);
        makeCurrent(bookingCancelled.booking());
        var bookingCancelledTicket =
                issuance.issueTicketsForConfirmedBooking(bookingCancelled.booking()).getFirst();
        bookingCancelled.booking().setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(bookingCancelled.booking());
        scan(staff, qrTokens.decryptToken(bookingCancelledTicket))
                .andExpect(jsonPath("$.data.result").value("BOOKING_CANCELLED"));
        Context late = context("late-ticket@example.com", 1, BookingStatus.CONFIRMED);
        var lateTicket = issuance.issueTicketsForConfirmedBooking(late.booking()).getFirst();
        var lateShow = late.booking().getShow();
        lateShow.setShowDate(LocalDate.now(clock).minusDays(1));
        lateShow.setStatus(com.chalchitraghar.modules.shows.enums.ShowStatus.COMPLETED);
        showRepository.save(lateShow);
        scan(staff, qrTokens.decryptToken(lateTicket))
                .andExpect(jsonPath("$.data.result").value("TOO_LATE"));
        scan(staff, "unknown-opaque-token").andExpect(jsonPath("$.data.result").value("INVALID"));
        assertThat(ticketValidationRepository.findAll())
                .extracting(v -> v.getResult())
                .contains(
                        ValidationResult.TOO_EARLY,
                        ValidationResult.SHOW_CANCELLED,
                        ValidationResult.REVOKED,
                        ValidationResult.EXPIRED,
                        ValidationResult.BOOKING_CANCELLED,
                        ValidationResult.TOO_LATE,
                        ValidationResult.INVALID);
        mockMvc.perform(
                        post("/api/staff/tickets/scan")
                                .header("Authorization", bearer(early.token()))
                                .contentType("application/json")
                                .content(
                                        json(
                                                java.util.Map.of(
                                                        "qrToken",
                                                        qrTokens.decryptToken(earlyTicket)))))
                .andExpect(status().isForbidden());
        mockMvc.perform(
                        post("/api/staff/tickets/scan")
                                .contentType("application/json")
                                .content(json(java.util.Map.of("qrToken", "x"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void concurrentScansProduceOneSuccessAndOneAlreadyUsed() throws Exception {
        Context c = context("concurrent-scan@example.com", 1, BookingStatus.CONFIRMED);
        makeCurrent(c.booking());
        var ticket = issuance.issueTicketsForConfirmedBooking(c.booking()).getFirst();
        String raw = qrTokens.decryptToken(ticket);
        User staff = saveUser("concurrent-staff@example.com", Role.STAFF);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var tasks =
                    List.of(
                            (java.util.concurrent.Callable<ValidationResult>)
                                    () ->
                                            validationService
                                                    .scan(
                                                            new TicketScanRequest(raw),
                                                            staff,
                                                            null,
                                                            null,
                                                            "a")
                                                    .result(),
                            () ->
                                    validationService
                                            .scan(
                                                    new TicketScanRequest(raw),
                                                    staff,
                                                    null,
                                                    null,
                                                    "b")
                                            .result());
            var results =
                    pool.invokeAll(tasks).stream()
                            .map(
                                    f -> {
                                        try {
                                            return f.get();
                                        } catch (Exception e) {
                                            throw new RuntimeException(e);
                                        }
                                    })
                            .toList();
            assertThat(results)
                    .containsExactlyInAnyOrder(
                            ValidationResult.SUCCESS, ValidationResult.ALREADY_USED);
        }
        assertThat(
                        ticketValidationRepository.findByTicketIdOrderByValidationTimeDesc(
                                ticket.getId()))
                .hasSize(2);
    }

    @Test
    void adminRevokesAndReissuesWhilePdfDownloadsRemainOwnerOnly() throws Exception {
        Context c = context("ticket4-owner@example.com", 2, BookingStatus.CONFIRMED);
        var issued = issuance.issueTicketsForConfirmedBooking(c.booking());
        var first = issued.getFirst();
        String old = qrTokens.decryptToken(first);
        String adminToken = tokenFor("ticket4-admin@example.com", Role.ADMIN);
        User admin = userRepository.findByEmail("ticket4-admin@example.com").orElseThrow();
        mockMvc.perform(
                        post("/api/admin/tickets/{ref}/reissue", first.getTicketReference())
                                .header("Authorization", bearer(adminToken))
                                .contentType("application/json")
                                .content("{\"reason\":\"QR exposed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticketReference").value(first.getTicketReference()));
        var rotated = ticketRepository.findById(first.getId()).orElseThrow();
        assertThat(rotated.getQrTokenVersion()).isEqualTo(2);
        assertThat(qrTokens.decryptToken(rotated)).isNotEqualTo(old);
        assertThat(ticketRepository.findByQrTokenHash(qrTokens.hashToken(old))).isEmpty();
        mockMvc.perform(
                        post("/api/admin/tickets/{ref}/revoke", rotated.getTicketReference())
                                .header("Authorization", bearer(adminToken))
                                .contentType("application/json")
                                .content("{\"reason\":\"Administrative cancellation\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REVOKED"));
        var revoked = ticketRepository.findById(first.getId()).orElseThrow();
        assertThat(revoked.getRevokedAt()).isNotNull();
        assertThat(revoked.getRevokedBy().getId()).isEqualTo(admin.getId());
        assertThat(revoked.getRevocationReason()).isEqualTo("Administrative cancellation");
        byte[] pdf =
                mockMvc.perform(
                                get("/api/customer/tickets/{ref}/pdf", first.getTicketReference())
                                        .header("Authorization", bearer(c.token())))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType("application/pdf"))
                        .andReturn()
                        .getResponse()
                        .getContentAsByteArray();
        PdfReader reader = new PdfReader(pdf);
        assertThat(reader.getNumberOfPages()).isEqualTo(1);
        String text = new PdfTextExtractor(reader).getTextFromPage(1);
        assertThat(text)
                .contains("Hamro Chalchitraghar", first.getTicketReference(), "REVOKED")
                .doesNotContain(rotated.getQrTokenHash(), rotated.getQrTokenEncrypted());
        reader.close();
        byte[] bundle =
                mockMvc.perform(
                                get(
                                                "/api/customer/bookings/{ref}/tickets/pdf",
                                                c.booking().getBookingReference())
                                        .header("Authorization", bearer(c.token())))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsByteArray();
        PdfReader bundled = new PdfReader(bundle);
        assertThat(bundled.getNumberOfPages()).isEqualTo(2);
        bundled.close();
        Context other = context("ticket4-other@example.com", 1, BookingStatus.CONFIRMED);
        mockMvc.perform(
                        get("/api/customer/tickets/{ref}/pdf", first.getTicketReference())
                                .header("Authorization", bearer(other.token())))
                .andExpect(status().isNotFound());
    }

    @Test
    void expiryJobExpiresOnlyIssuedTicketsAndDeliveryIsIdempotentlyQueued() throws Exception {
        Context c = context("expiry-delivery@example.com", 2, BookingStatus.CONFIRMED);
        var list = issuance.issueTicketsForConfirmedBooking(c.booking());
        var show = c.booking().getShow();
        show.setShowDate(LocalDate.now(clock).minusDays(1));
        showRepository.save(show);
        list.get(1).setStatus(TicketStatus.REVOKED);
        ticketRepository.save(list.get(1));
        assertThat(expiryJob.run()).isEqualTo(1);
        assertThat(expiryJob.run()).isZero();
        assertThat(ticketRepository.findById(list.get(0).getId()).orElseThrow().getStatus())
                .isEqualTo(TicketStatus.EXPIRED);
        assertThat(ticketRepository.findById(list.get(1).getId()).orElseThrow().getStatus())
                .isEqualTo(TicketStatus.REVOKED);
        issuance.issueTicketsForConfirmedBooking(c.booking());
        assertThat(ticketDeliveryRepository.count()).isEqualTo(1);
    }

    private org.springframework.test.web.servlet.ResultActions scan(String token, String raw)
            throws Exception {
        return mockMvc.perform(
                        post("/api/staff/tickets/scan")
                                .header("Authorization", bearer(token))
                                .contentType("application/json")
                                .content(json(java.util.Map.of("qrToken", raw))))
                .andExpect(status().isOk());
    }

    private void makeCurrent(Booking booking) {
        var show = booking.getShow();
        show.setShowDate(LocalDate.now(clock));
        show.setShowTime(LocalTime.now(clock).minusMinutes(30));
        show.setEndTime(LocalTime.now(clock).plusMinutes(90));
        show.setStatus(com.chalchitraghar.modules.shows.enums.ShowStatus.RUNNING);
        showRepository.save(show);
    }

    private Context context(String email, int seatCount, BookingStatus status) throws Exception {
        User user = saveUser(email, Role.CUSTOMER);
        String token = loginToken(email);
        String admin = tokenFor("admin-" + email, Role.ADMIN);
        var show =
                saveShowWithSeats(
                        saveMovie(
                                "Movie " + email,
                                com.chalchitraghar.modules.movies.enums.MovieStatus.NOW_SHOWING),
                        saveHall(
                                "Hall " + email,
                                com.chalchitraghar.modules.halls.enums.Status.ACTIVE),
                        admin);
        var selected = seatsForShow(show.getId()).subList(0, seatCount);
        selected.forEach(
                s ->
                        s.setSeatStatus(
                                status == BookingStatus.CONFIRMED
                                        ? SeatStatus.BOOKED
                                        : SeatStatus.RESERVED));
        seatRepository.saveAll(selected);
        BigDecimal total =
                selected.stream().map(s -> s.getPrice()).reduce(BigDecimal.ZERO, BigDecimal::add);
        Booking booking =
                bookingRepository.save(
                        Booking.builder()
                                .user(user)
                                .show(show)
                                .bookingReference("HCG-20260712-" + Math.abs(email.hashCode()))
                                .bookingTime(LocalDateTime.now(clock))
                                .status(status)
                                .totalAmount(total)
                                .currency("NPR")
                                .confirmedAt(
                                        status == BookingStatus.CONFIRMED
                                                ? LocalDateTime.now(clock)
                                                : null)
                                .expiresAt(LocalDateTime.now(clock).plusMinutes(15))
                                .build());
        List<BookingSeat> claims =
                selected.stream()
                        .map(
                                s ->
                                        BookingSeat.builder()
                                                .booking(booking)
                                                .seat(s)
                                                .unitPrice(s.getPrice())
                                                .build())
                        .toList();
        bookingSeatRepository.saveAll(claims);
        return new Context(user, token, booking);
    }

    private record Context(User user, String token, Booking booking) {}
}
