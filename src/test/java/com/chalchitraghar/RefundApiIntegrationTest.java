package com.chalchitraghar;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.chalchitraghar.modules.bookings.entity.Booking;
import com.chalchitraghar.modules.bookings.entity.BookingSeat;
import com.chalchitraghar.modules.bookings.enums.BookingStatus;
import com.chalchitraghar.modules.halls.entity.Hall;
import com.chalchitraghar.modules.halls.enums.Status;
import com.chalchitraghar.modules.movies.entity.Movie;
import com.chalchitraghar.modules.movies.enums.MovieStatus;
import com.chalchitraghar.modules.payments.dto.request.CreateRefundIntentCommand;
import com.chalchitraghar.modules.payments.entity.Payment;
import com.chalchitraghar.modules.payments.enums.*;
import com.chalchitraghar.modules.payments.service.RefundService;
import com.chalchitraghar.modules.seats.entity.Seat;
import com.chalchitraghar.modules.seats.enums.*;
import com.chalchitraghar.modules.shows.entity.Show;
import com.chalchitraghar.modules.shows.enums.ShowStatus;
import com.chalchitraghar.modules.tickets.entity.Ticket;
import com.chalchitraghar.modules.tickets.enums.TicketStatus;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.modules.users.enums.Role;
import com.chalchitraghar.shared.exception.PaymentConflictException;
import java.math.BigDecimal;
import java.time.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RefundApiIntegrationTest extends AbstractIntegrationTest {
    @Autowired RefundService refundService;

    @Test
    void createsFullRequestedIntentIdempotentlyWithoutMutatingRelatedState() {
        Fixture f =
                fixture(
                        "refund-owner@example.com",
                        Role.CUSTOMER,
                        PaymentStatus.SUCCESS,
                        ShowStatus.CANCELLED);
        var command =
                new CreateRefundIntentCommand(
                        f.payment().getPaymentReference(),
                        RefundReason.SHOW_CANCELLATION,
                        "SHOW_CANCELLATION:" + f.booking().getId() + ":" + f.show().getId(),
                        f.user().getId());

        var first = refundService.createRefundIntent(command);
        var repeated = refundService.createRefundIntent(command);

        assertThat(repeated.getId()).isEqualTo(first.getId());
        assertThat(first.getRefundReference()).matches("RFD-\\d{8}-[A-F0-9]{8}");
        assertThat(first.getStatus()).isEqualTo(RefundStatus.REQUESTED);
        assertThat(first.getType()).isEqualTo(RefundType.FULL);
        assertThat(first.getMethod()).isEqualTo(RefundMethod.MANUAL);
        assertThat(first.getAmount()).isEqualByComparingTo(f.payment().getAmount());
        assertThat(first.getCurrency()).isEqualTo("NPR");
        assertThat(first.getRequestedAt())
                .isBetween(
                        LocalDateTime.now(clock).minusSeconds(5),
                        LocalDateTime.now(clock).plusSeconds(5));
        assertThat(paymentRepository.findById(f.payment().getId()).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.SUCCESS);
        assertThat(bookingRepository.findById(f.booking().getId()).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.CONFIRMED);
        assertThat(showRepository.findById(f.show().getId()).orElseThrow().getStatus())
                .isEqualTo(ShowStatus.CANCELLED);
        assertThat(refundRepository.count()).isOne();
        assertThat(notificationRepository.count()).isOne();
    }

    @Test
    void rejectsNonSuccessfulPaymentsAndOverRefunds() {
        Fixture pending =
                fixture(
                        "pending-refund@example.com",
                        Role.CUSTOMER,
                        PaymentStatus.PENDING,
                        ShowStatus.CANCELLED);
        assertThatThrownBy(() -> refundService.createRefundIntent(command(pending, "PENDING")))
                .isInstanceOf(PaymentConflictException.class)
                .hasMessageContaining("successful");

        Fixture success =
                fixture(
                        "over-refund@example.com",
                        Role.CUSTOMER,
                        PaymentStatus.SUCCESS,
                        ShowStatus.CANCELLED);
        refundService.createRefundIntent(command(success, "FIRST"));
        assertThatThrownBy(() -> refundService.createRefundIntent(command(success, "SECOND")))
                .isInstanceOf(PaymentConflictException.class)
                .hasMessageContaining("refundable balance");
    }

    @Test
    void enforcesReasonStateAndDerivesFinancialValues() {
        Fixture scheduled =
                fixture(
                        "reason-refund@example.com",
                        Role.CUSTOMER,
                        PaymentStatus.SUCCESS,
                        ShowStatus.SCHEDULED);
        assertThatThrownBy(() -> refundService.createRefundIntent(command(scheduled, "WRONG_SHOW")))
                .isInstanceOf(PaymentConflictException.class)
                .hasMessageContaining("cancelled show");
        var admin =
                refundService.createRefundIntent(
                        new CreateRefundIntentCommand(
                                scheduled.payment().getPaymentReference(),
                                RefundReason.ADMIN_ADJUSTMENT,
                                "ADMIN_ADJUSTMENT:" + scheduled.payment().getId(),
                                null));
        assertThat(admin.getAmount()).isEqualByComparingTo("1500.00");
        assertThat(admin.getCurrency()).isEqualTo("NPR");
    }

    @Test
    void checkedInTicketMakesIntentIneligible() {
        Fixture f =
                fixture(
                        "checked-in-refund@example.com",
                        Role.CUSTOMER,
                        PaymentStatus.SUCCESS,
                        ShowStatus.CANCELLED);
        Seat seat =
                seatRepository.save(
                        Seat.builder()
                                .show(f.show())
                                .seatNumber(1)
                                .rowLabel("A")
                                .seatCode("A1")
                                .seatType(SeatType.PREMIUM)
                                .price(new BigDecimal("1500.00"))
                                .seatStatus(SeatStatus.BOOKED)
                                .positionIndex(1)
                                .build());
        seat.setSeatStatus(SeatStatus.BOOKED);
        seat = seatRepository.save(seat);
        BookingSeat bookingSeat =
                bookingSeatRepository.save(
                        BookingSeat.builder()
                                .booking(f.booking())
                                .seat(seat)
                                .unitPrice(new BigDecimal("1500.00"))
                                .build());
        LocalDateTime now = LocalDateTime.now(clock);
        ticketRepository.save(
                Ticket.builder()
                        .booking(f.booking())
                        .bookingSeat(bookingSeat)
                        .ticketReference("TKT-REFUND-CHECKED")
                        .status(TicketStatus.CHECKED_IN)
                        .issuedAt(now)
                        .checkedInAt(now)
                        .checkedInBy(f.user())
                        .qrTokenVersion(1)
                        .qrKeyId("test")
                        .qrTokenEncrypted("encrypted")
                        .qrTokenHash("a".repeat(128))
                        .qrIssuedAt(now)
                        .build());

        assertThatThrownBy(() -> refundService.createRefundIntent(command(f, "CHECKED_IN")))
                .isInstanceOf(PaymentConflictException.class)
                .hasMessageContaining("Checked-in");
    }

    @Test
    void concurrentDifferentKeysReserveAtMostOneFullAmount() throws Exception {
        Fixture f =
                fixture(
                        "concurrent-refund@example.com",
                        Role.CUSTOMER,
                        PaymentStatus.SUCCESS,
                        ShowStatus.CANCELLED);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> first =
                    executor.submit(() -> attemptAfter(start, command(f, "CONCURRENT_A")));
            Future<Boolean> second =
                    executor.submit(() -> attemptAfter(start, command(f, "CONCURRENT_B")));
            start.countDown();
            assertThat(java.util.List.of(first.get(), second.get()))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(refundRepository.count()).isOne();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void customerReadsOnlyOwnedRefundsAndSensitiveFieldsAreAbsent() throws Exception {
        Fixture owner =
                fixture(
                        "owner-read@example.com",
                        Role.CUSTOMER,
                        PaymentStatus.SUCCESS,
                        ShowStatus.CANCELLED);
        Fixture other =
                fixture(
                        "other-read@example.com",
                        Role.CUSTOMER,
                        PaymentStatus.SUCCESS,
                        ShowStatus.CANCELLED);
        var refund = refundService.createRefundIntent(command(owner, "OWNER"));
        refundService.createRefundIntent(command(other, "OTHER"));
        String token = loginToken(owner.user().getEmail());

        mockMvc.perform(get("/api/customer/refunds").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(
                        jsonPath("$.data.content[0].refundReference")
                                .value(refund.getRefundReference()))
                .andExpect(jsonPath("$.data.content[0].idempotencyKey").doesNotExist());
        mockMvc.perform(
                        get("/api/customer/refunds/{reference}", refund.getRefundReference())
                                .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.safeStatusMessage")
                                .value(org.hamcrest.Matchers.containsString("no money")))
                .andExpect(jsonPath("$.data.requestedBy").doesNotExist());
        mockMvc.perform(
                        get(
                                        "/api/customer/refunds/{reference}",
                                        refundRepository
                                                .findByPaymentIdOrderByRequestedAtDescIdDesc(
                                                        other.payment().getId())
                                                .getFirst()
                                                .getRefundReference())
                                .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void bookingLookupIsOwnerScopedAndFiltersAreValidated() throws Exception {
        Fixture owner =
                fixture(
                        "booking-refund@example.com",
                        Role.CUSTOMER,
                        PaymentStatus.SUCCESS,
                        ShowStatus.CANCELLED);
        Fixture other =
                fixture(
                        "booking-other@example.com",
                        Role.CUSTOMER,
                        PaymentStatus.SUCCESS,
                        ShowStatus.CANCELLED);
        refundService.createRefundIntent(command(owner, "BOOKING"));
        String token = loginToken(owner.user().getEmail());

        mockMvc.perform(
                        get(
                                        "/api/customer/bookings/{reference}/refunds",
                                        owner.booking().getBookingReference())
                                .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
        mockMvc.perform(
                        get(
                                        "/api/customer/bookings/{reference}/refunds",
                                        other.booking().getBookingReference())
                                .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                        get("/api/customer/refunds")
                                .param("requestedFrom", "2026-07-16T00:00:00")
                                .param("requestedTo", "2026-07-15T00:00:00")
                                .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(
                        get("/api/customer/refunds")
                                .param("status", "PAID")
                                .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void staffAndAdminCustomerRoutesRemainOwnerScoped() throws Exception {
        Fixture staff =
                fixture(
                        "staff-owned-refund@example.com",
                        Role.STAFF,
                        PaymentStatus.SUCCESS,
                        ShowStatus.CANCELLED);
        Fixture admin =
                fixture(
                        "admin-owned-refund@example.com",
                        Role.ADMIN,
                        PaymentStatus.SUCCESS,
                        ShowStatus.CANCELLED);
        Fixture other =
                fixture(
                        "customer-route-other@example.com",
                        Role.CUSTOMER,
                        PaymentStatus.SUCCESS,
                        ShowStatus.CANCELLED);
        var staffRefund = refundService.createRefundIntent(command(staff, "STAFF_OWN"));
        var adminRefund = refundService.createRefundIntent(command(admin, "ADMIN_OWN"));
        var otherRefund = refundService.createRefundIntent(command(other, "OTHER_OWN"));

        String staffToken = loginToken(staff.user().getEmail());
        String adminToken = loginToken(admin.user().getEmail());
        mockMvc.perform(get("/api/customer/refunds").header("Authorization", bearer(staffToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(
                        jsonPath("$.data.content[0].refundReference")
                                .value(staffRefund.getRefundReference()));
        mockMvc.perform(get("/api/customer/refunds").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(
                        jsonPath("$.data.content[0].refundReference")
                                .value(adminRefund.getRefundReference()));
        mockMvc.perform(
                        get("/api/customer/refunds/{reference}", otherRefund.getRefundReference())
                                .header("Authorization", bearer(staffToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminReadsAllWhileCustomerAndStaffAreForbidden() throws Exception {
        Fixture f =
                fixture(
                        "admin-visible-refund@example.com",
                        Role.CUSTOMER,
                        PaymentStatus.SUCCESS,
                        ShowStatus.CANCELLED);
        var refund = refundService.createRefundIntent(command(f, "ADMIN_READ"));
        String admin = tokenFor("refund-admin@example.com", Role.ADMIN);
        String staff = tokenFor("refund-staff@example.com", Role.STAFF);
        String customer = loginToken(f.user().getEmail());

        mockMvc.perform(
                        get("/api/admin/refunds")
                                .param("reason", "SHOW_CANCELLATION")
                                .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
        mockMvc.perform(
                        get("/api/admin/refunds/{reference}", refund.getRefundReference())
                                .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentProvider").value("ESEWA"))
                .andExpect(jsonPath("$.data.idempotencyKey").doesNotExist());
        mockMvc.perform(get("/api/admin/refunds").header("Authorization", bearer(customer)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/refunds").header("Authorization", bearer(staff)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/refunds")).andExpect(status().isUnauthorized());
    }

    private CreateRefundIntentCommand command(Fixture f, String suffix) {
        return new CreateRefundIntentCommand(
                f.payment().getPaymentReference(),
                RefundReason.SHOW_CANCELLATION,
                "SHOW_CANCELLATION:" + f.booking().getId() + ":" + suffix,
                f.user().getId());
    }

    private boolean attemptAfter(CountDownLatch start, CreateRefundIntentCommand command)
            throws InterruptedException {
        start.await();
        try {
            refundService.createRefundIntent(command);
            return true;
        } catch (PaymentConflictException expected) {
            return false;
        }
    }

    private Fixture fixture(
            String email, Role role, PaymentStatus paymentStatus, ShowStatus showStatus) {
        User user = saveUser(email, role);
        Movie movie = saveMovie("Refund " + email, MovieStatus.NOW_SHOWING);
        Hall hall = saveHall("Refund Hall " + email, Status.ACTIVE);
        Show show =
                showRepository.save(
                        Show.builder()
                                .movie(movie)
                                .hall(hall)
                                .status(showStatus)
                                .showDate(LocalDate.now(clock).plusDays(5))
                                .showTime(LocalTime.of(10, 0))
                                .endTime(LocalTime.NOON)
                                .build());
        show.setStatus(showStatus);
        show = showRepository.save(show);
        Booking booking =
                bookingRepository.save(
                        Booking.builder()
                                .bookingReference("HCG-REFUND-" + user.getId())
                                .user(user)
                                .show(show)
                                .bookingTime(LocalDateTime.now(clock))
                                .status(BookingStatus.CONFIRMED)
                                .totalAmount(new BigDecimal("1500.00"))
                                .currency("NPR")
                                .confirmedAt(LocalDateTime.now(clock))
                                .build());
        Payment payment =
                paymentRepository.save(
                        Payment.builder()
                                .booking(booking)
                                .paymentReference("PAY-REFUND-" + user.getId())
                                .provider(PaymentProvider.ESEWA)
                                .method(PaymentMethod.ONLINE)
                                .status(paymentStatus)
                                .amount(new BigDecimal("1500.00"))
                                .currency("NPR")
                                .initiatedAt(LocalDateTime.now(clock))
                                .build());
        return new Fixture(user, show, booking, payment);
    }

    private record Fixture(User user, Show show, Booking booking, Payment payment) {}
}
