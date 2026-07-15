package com.chalchitraghar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.chalchitraghar.modules.audit.enums.AuditAction;
import com.chalchitraghar.modules.bookings.entity.*;
import com.chalchitraghar.modules.bookings.enums.BookingStatus;
import com.chalchitraghar.modules.halls.entity.Hall;
import com.chalchitraghar.modules.halls.enums.Status;
import com.chalchitraghar.modules.movies.entity.Movie;
import com.chalchitraghar.modules.movies.enums.MovieStatus;
import com.chalchitraghar.modules.notifications.enums.NotificationType;
import com.chalchitraghar.modules.payments.entity.Payment;
import com.chalchitraghar.modules.payments.enums.*;
import com.chalchitraghar.modules.seats.entity.Seat;
import com.chalchitraghar.modules.seats.enums.*;
import com.chalchitraghar.modules.shows.entity.Show;
import com.chalchitraghar.modules.shows.enums.ShowStatus;
import com.chalchitraghar.modules.tickets.entity.Ticket;
import com.chalchitraghar.modules.tickets.enums.TicketStatus;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.modules.users.enums.Role;
import java.math.BigDecimal;
import java.time.*;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RefundWorkflowIntegrationTest extends AbstractIntegrationTest {

    @Test
    void manualProcessingRequiresConfirmationThenFinalizesPaymentIdempotently() throws Exception {
        Fixture f = confirmedFixture("workflow-processing@example.com", TicketStatus.ISSUED);
        String admin = tokenFor("workflow-processing-admin@example.com", Role.ADMIN);
        String reference = createAdminRefund(admin, f.payment(), "PROCESS-MANUAL-1");
        mockMvc.perform(
                        post("/api/admin/refunds/{reference}/approve", reference)
                                .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());

        mockMvc.perform(
                        post("/api/admin/refunds/{reference}/process", reference)
                                .header("Authorization", bearer(admin)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.status").value("MANUAL_REVIEW"));
        assertThat(paymentRepository.findById(f.payment().getId()).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.SUCCESS);
        assertThat(refundAttemptRepository.findAll())
                .singleElement()
                .satisfies(
                        a ->
                                assertThat(a.getStatus())
                                        .isEqualTo(
                                                com.chalchitraghar.modules.payments.enums
                                                        .RefundAttemptStatus.UNKNOWN));

        String body =
                json(
                        Map.of(
                                "externalReference",
                                "MANUAL-RECEIPT-1",
                                "note",
                                "Verified outside the system"));
        mockMvc.perform(
                        post("/api/admin/refunds/{reference}/mark-manual-success", reference)
                                .header("Authorization", bearer(admin))
                                .contentType("application/json")
                                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.paymentStatus").value("REFUNDED"));
        mockMvc.perform(
                        post("/api/admin/refunds/{reference}/mark-manual-success", reference)
                                .header("Authorization", bearer(admin))
                                .contentType("application/json")
                                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"));
        assertThat(paymentRepository.findById(f.payment().getId()).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.REFUNDED);
        assertThat(refundAttemptRepository.findAll())
                .singleElement()
                .satisfies(
                        a ->
                                assertThat(a.getStatus())
                                        .isEqualTo(
                                                com.chalchitraghar.modules.payments.enums
                                                        .RefundAttemptStatus.SUCCEEDED));

        mockMvc.perform(
                        get("/api/admin/refunds/{reference}/attempts", reference)
                                .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void customerRequestAtomicallyCancelsBookingRevokesTicketAndCreatesPendingIntent()
            throws Exception {
        Fixture f = confirmedFixture("workflow-customer@example.com", TicketStatus.ISSUED);
        String token = loginToken(f.user().getEmail());

        mockMvc.perform(
                        post(
                                        "/api/customer/bookings/{reference}/refund-request",
                                        f.booking().getBookingReference())
                                .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookingStatus").value("CANCELLED"))
                .andExpect(jsonPath("$.data.refundStatus").value("REQUESTED"))
                .andExpect(
                        jsonPath("$.data.message")
                                .value(org.hamcrest.Matchers.containsString("no money")));

        var refund =
                refundRepository
                        .findByPaymentIdOrderByRequestedAtDescIdDesc(f.payment().getId())
                        .getFirst();
        assertThat(refund.getAmount()).isEqualByComparingTo(f.payment().getAmount());
        assertThat(paymentRepository.findById(f.payment().getId()).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.SUCCESS);
        assertThat(bookingRepository.findById(f.booking().getId()).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.CANCELLED);
        assertThat(ticketRepository.findById(f.ticket().getId()).orElseThrow().getStatus())
                .isEqualTo(TicketStatus.REVOKED);
        assertThat(seatRepository.findById(f.seat().getId()).orElseThrow().getSeatStatus())
                .isEqualTo(SeatStatus.AVAILABLE);
        assertThat(notificationRepository.findAll())
                .extracting(n -> n.getType())
                .contains(NotificationType.REFUND_REQUESTED, NotificationType.BOOKING_CANCELLED);
        assertThat(auditLogRepository.findAll())
                .extracting(a -> a.getAction())
                .contains(AuditAction.REFUND_CREATED, AuditAction.BOOKING_CANCELLED);

        mockMvc.perform(
                        post(
                                        "/api/customer/bookings/{reference}/refund-request",
                                        f.booking().getBookingReference())
                                .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refundReference").value(refund.getRefundReference()));
        assertThat(refundRepository.count()).isOne();
    }

    @Test
    void checkedInTicketRollsBackCustomerRequest() throws Exception {
        Fixture f = confirmedFixture("workflow-checked@example.com", TicketStatus.CHECKED_IN);
        String token = loginToken(f.user().getEmail());

        mockMvc.perform(
                        post(
                                        "/api/customer/bookings/{reference}/refund-request",
                                        f.booking().getBookingReference())
                                .header("Authorization", bearer(token)))
                .andExpect(status().isConflict());

        assertThat(refundRepository.count()).isZero();
        assertThat(bookingRepository.findById(f.booking().getId()).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.CONFIRMED);
        assertThat(ticketRepository.findById(f.ticket().getId()).orElseThrow().getStatus())
                .isEqualTo(TicketStatus.CHECKED_IN);
        assertThat(seatRepository.findById(f.seat().getId()).orElseThrow().getSeatStatus())
                .isEqualTo(SeatStatus.BOOKED);
    }

    @Test
    void adminCreatesApprovesAndRejectsWithoutMovingMoney() throws Exception {
        Fixture approvedFixture =
                confirmedFixture("workflow-approved@example.com", TicketStatus.ISSUED);
        Fixture rejectedFixture =
                confirmedFixture("workflow-rejected@example.com", TicketStatus.ISSUED);
        String admin = tokenFor("workflow-admin@example.com", Role.ADMIN);

        String approvedReference =
                createAdminRefund(admin, approvedFixture.payment(), "ADMIN-APPROVE-1");
        mockMvc.perform(
                        post("/api/admin/refunds/{reference}/approve", approvedReference)
                                .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.approvedAt").isNotEmpty());
        mockMvc.perform(
                        post("/api/admin/refunds/{reference}/approve", approvedReference)
                                .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        String rejectedReference =
                createAdminRefund(admin, rejectedFixture.payment(), "ADMIN-REJECT-1");
        mockMvc.perform(
                        post("/api/admin/refunds/{reference}/reject", rejectedReference)
                                .header("Authorization", bearer(admin))
                                .contentType("application/json")
                                .content(
                                        json(
                                                Map.of(
                                                        "reasonCode",
                                                        "POLICY_DENIED",
                                                        "note",
                                                        "Reviewed safely"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectionReasonCode").value("POLICY_DENIED"));
        mockMvc.perform(
                        post("/api/admin/refunds/{reference}/approve", rejectedReference)
                                .header("Authorization", bearer(admin)))
                .andExpect(status().isConflict());

        assertThat(
                        paymentRepository
                                .findById(approvedFixture.payment().getId())
                                .orElseThrow()
                                .getStatus())
                .isEqualTo(PaymentStatus.SUCCESS);
        assertThat(notificationRepository.findAll())
                .extracting(n -> n.getType())
                .contains(
                        NotificationType.REFUND_REQUESTED,
                        NotificationType.REFUND_APPROVED,
                        NotificationType.REFUND_REJECTED);
        assertThat(auditLogRepository.findAll())
                .extracting(a -> a.getAction())
                .contains(
                        AuditAction.REFUND_CREATED,
                        AuditAction.REFUND_APPROVED,
                        AuditAction.REFUND_REJECTED);

        String customer = loginToken(approvedFixture.user().getEmail());
        String staff = tokenFor("workflow-refund-staff@example.com", Role.STAFF);
        mockMvc.perform(
                        post("/api/admin/refunds")
                                .header("Authorization", bearer(customer))
                                .header("Idempotency-Key", "DENIED-CUSTOMER")
                                .contentType("application/json")
                                .content(
                                        json(
                                                Map.of(
                                                        "paymentReference",
                                                        approvedFixture
                                                                .payment()
                                                                .getPaymentReference(),
                                                        "reason",
                                                        "ADMIN_ADJUSTMENT"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(
                        post("/api/admin/refunds/{reference}/approve", approvedReference)
                                .header("Authorization", bearer(staff)))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonOwnerAndMissingSuccessfulPaymentAreRejectedWithoutSideEffects() throws Exception {
        Fixture f = confirmedFixture("workflow-ineligible@example.com", TicketStatus.ISSUED);
        String other = tokenFor("workflow-other-owner@example.com", Role.CUSTOMER);
        mockMvc.perform(
                        post(
                                        "/api/customer/bookings/{reference}/refund-request",
                                        f.booking().getBookingReference())
                                .header("Authorization", bearer(other)))
                .andExpect(status().isNotFound());

        f.payment().setStatus(PaymentStatus.FAILED);
        paymentRepository.save(f.payment());
        String owner = loginToken(f.user().getEmail());
        mockMvc.perform(
                        post(
                                        "/api/customer/bookings/{reference}/refund-request",
                                        f.booking().getBookingReference())
                                .header("Authorization", bearer(owner)))
                .andExpect(status().isConflict());
        assertThat(refundRepository.count()).isZero();
        assertThat(bookingRepository.findById(f.booking().getId()).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void showCancellationCancelsConfirmedBookingAndAutoApprovesRefund() throws Exception {
        Fixture f = confirmedFixture("workflow-show@example.com", TicketStatus.ISSUED);
        String admin = tokenFor("workflow-show-admin@example.com", Role.ADMIN);

        mockMvc.perform(
                        delete("/api/admin/shows/{id}", f.show().getId())
                                .header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());

        assertThat(showRepository.findById(f.show().getId()).orElseThrow().getStatus())
                .isEqualTo(ShowStatus.CANCELLED);
        assertThat(bookingRepository.findById(f.booking().getId()).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.CANCELLED);
        assertThat(ticketRepository.findById(f.ticket().getId()).orElseThrow().getStatus())
                .isEqualTo(TicketStatus.REVOKED);
        assertThat(
                        refundRepository.findByPaymentIdOrderByRequestedAtDescIdDesc(
                                f.payment().getId()))
                .singleElement()
                .satisfies(r -> assertThat(r.getStatus()).isEqualTo(RefundStatus.APPROVED));

        mockMvc.perform(
                        delete("/api/admin/shows/{id}", f.show().getId())
                                .header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());
        assertThat(refundRepository.count()).isOne();
    }

    @Test
    void showCancellationWithCheckedInTicketRollsBackEverything() throws Exception {
        Fixture f = confirmedFixture("workflow-show-checked@example.com", TicketStatus.CHECKED_IN);
        String admin = tokenFor("workflow-show-checked-admin@example.com", Role.ADMIN);

        mockMvc.perform(
                        delete("/api/admin/shows/{id}", f.show().getId())
                                .header("Authorization", bearer(admin)))
                .andExpect(status().isConflict());

        assertThat(showRepository.findById(f.show().getId()).orElseThrow().getStatus())
                .isEqualTo(ShowStatus.SCHEDULED);
        assertThat(bookingRepository.findById(f.booking().getId()).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.CONFIRMED);
        assertThat(ticketRepository.findById(f.ticket().getId()).orElseThrow().getStatus())
                .isEqualTo(TicketStatus.CHECKED_IN);
        assertThat(refundRepository.count()).isZero();
    }

    private String createAdminRefund(String token, Payment payment, String key) throws Exception {
        var result =
                mockMvc.perform(
                                post("/api/admin/refunds")
                                        .header("Authorization", bearer(token))
                                        .header("Idempotency-Key", key)
                                        .contentType("application/json")
                                        .content(
                                                json(
                                                        Map.of(
                                                                "paymentReference",
                                                                payment.getPaymentReference(),
                                                                "reason",
                                                                "ADMIN_ADJUSTMENT"))))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.status").value("REQUESTED"))
                        .andReturn();
        return objectMapper
                .readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("refundReference")
                .asText();
    }

    private Fixture confirmedFixture(String email, TicketStatus ticketStatus) {
        User user = saveUser(email, Role.CUSTOMER);
        Movie movie = saveMovie("Refund Workflow " + email, MovieStatus.NOW_SHOWING);
        Hall hall = saveHall("Refund Workflow Hall " + email, Status.ACTIVE);
        Show show =
                showRepository.save(
                        Show.builder()
                                .movie(movie)
                                .hall(hall)
                                .status(ShowStatus.SCHEDULED)
                                .showDate(LocalDate.now(clock).plusDays(5))
                                .showTime(LocalTime.of(10, 0))
                                .endTime(LocalTime.NOON)
                                .build());
        Seat seat =
                seatRepository.save(
                        Seat.builder()
                                .show(show)
                                .seatNumber(1)
                                .rowLabel("A")
                                .seatCode("A1")
                                .seatType(SeatType.PREMIUM)
                                .price(new BigDecimal("1200.00"))
                                .seatStatus(SeatStatus.BOOKED)
                                .positionIndex(1)
                                .build());
        seat.setSeatStatus(SeatStatus.BOOKED);
        seat = seatRepository.save(seat);
        Booking booking =
                bookingRepository.save(
                        Booking.builder()
                                .bookingReference("HCG-WORKFLOW-" + user.getId())
                                .user(user)
                                .show(show)
                                .bookingTime(LocalDateTime.now(clock))
                                .status(BookingStatus.CONFIRMED)
                                .totalAmount(new BigDecimal("1200.00"))
                                .currency("NPR")
                                .confirmedAt(LocalDateTime.now(clock))
                                .build());
        BookingSeat claim =
                bookingSeatRepository.save(
                        BookingSeat.builder()
                                .booking(booking)
                                .seat(seat)
                                .unitPrice(new BigDecimal("1200.00"))
                                .build());
        Payment payment =
                paymentRepository.save(
                        Payment.builder()
                                .booking(booking)
                                .paymentReference("PAY-WORKFLOW-" + user.getId())
                                .provider(PaymentProvider.ESEWA)
                                .method(PaymentMethod.ONLINE)
                                .status(PaymentStatus.SUCCESS)
                                .amount(new BigDecimal("1200.00"))
                                .currency("NPR")
                                .initiatedAt(LocalDateTime.now(clock))
                                .completedAt(LocalDateTime.now(clock))
                                .build());
        LocalDateTime now = LocalDateTime.now(clock);
        Ticket ticket =
                ticketRepository.save(
                        Ticket.builder()
                                .booking(booking)
                                .bookingSeat(claim)
                                .ticketReference("TKT-WORKFLOW-" + user.getId())
                                .status(ticketStatus)
                                .issuedAt(now)
                                .checkedInAt(ticketStatus == TicketStatus.CHECKED_IN ? now : null)
                                .checkedInBy(ticketStatus == TicketStatus.CHECKED_IN ? user : null)
                                .qrTokenVersion(1)
                                .qrKeyId("test")
                                .qrTokenEncrypted("encrypted-" + user.getId())
                                .qrTokenHash(String.format("%0128d", user.getId()))
                                .qrIssuedAt(now)
                                .build());
        return new Fixture(user, show, seat, booking, payment, ticket);
    }

    private record Fixture(
            User user, Show show, Seat seat, Booking booking, Payment payment, Ticket ticket) {}
}
