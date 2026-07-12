package com.chalchitraghar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.chalchitraghar.modules.bookings.entity.Booking;
import com.chalchitraghar.modules.bookings.entity.BookingSeat;
import com.chalchitraghar.modules.bookings.enums.BookingStatus;
import com.chalchitraghar.modules.seats.enums.SeatStatus;
import com.chalchitraghar.modules.tickets.enums.TicketStatus;
import com.chalchitraghar.modules.tickets.service.TicketIssuanceService;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.modules.users.enums.Role;
import com.chalchitraghar.shared.exception.InvalidBookingStateException;

class TicketApiIntegrationTest extends AbstractIntegrationTest {
    @Autowired TicketIssuanceService issuance;

    @Test
    void confirmedTwoSeatBookingIssuesExactlyOneTicketPerSeatIdempotently() throws Exception {
        Context c = context("ticket-owner@example.com", 2, BookingStatus.CONFIRMED);
        LocalDateTime beforeIssuance = LocalDateTime.now(clock);
        var first = issuance.issueTicketsForConfirmedBooking(c.booking());
        LocalDateTime afterIssuance = LocalDateTime.now(clock);
        var second = issuance.issueTicketsForConfirmedBooking(c.booking());

        assertThat(first).hasSize(2);
        assertThat(second).extracting(t -> t.getId()).containsExactlyElementsOf(first.stream().map(t -> t.getId()).toList());
        assertThat(ticketRepository.count()).isEqualTo(2);
        assertThat(first).extracting(t -> t.getBookingSeat().getSeat().getPositionIndex()).isSorted();
        assertThat(first).allSatisfy(t -> {
            assertThat(t.getStatus()).isEqualTo(TicketStatus.ISSUED);
            assertThat(t.getIssuedAt()).isBetween(beforeIssuance, afterIssuance);
            assertThat(t.getTicketReference()).matches("TKT-\\d{8}-[A-Z0-9]{8}");
            assertThat(t.getBookingSeat().getSeat().getSeatStatus()).isEqualTo(SeatStatus.BOOKED);
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
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].customerId").doesNotExist())
                .andExpect(jsonPath("$.data[0].qrTokenVersion").doesNotExist());
        mockMvc.perform(get("/api/customer/tickets/{ref}", owned.getFirst().getTicketReference())
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.seatCode").exists())
                .andExpect(jsonPath("$.data.customerEmail").doesNotExist())
                .andExpect(jsonPath("$.data.id").doesNotExist());
        mockMvc.perform(get("/api/customer/tickets/{ref}", foreign.getTicketReference())
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/customer/bookings/{ref}/tickets", owner.booking().getBookingReference())
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void staffAndAdminRoutesEnforceAudienceAndExposeOperationalIdentity() throws Exception {
        Context c = context("ticket-roles-owner@example.com", 1, BookingStatus.CONFIRMED);
        var ticket = issuance.issueTicketsForConfirmedBooking(c.booking()).getFirst();
        String staff = tokenFor("ticket-staff@example.com", Role.STAFF);
        String admin = tokenFor("ticket-admin@example.com", Role.ADMIN);

        mockMvc.perform(get("/api/staff/tickets/{ref}", ticket.getTicketReference()).header("Authorization", bearer(staff)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.customerEmail").value(c.user().getEmail()));
        mockMvc.perform(get("/api/staff/bookings/{ref}/tickets", c.booking().getBookingReference()).header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1));
        mockMvc.perform(get("/api/admin/tickets/{ref}", ticket.getTicketReference()).header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/staff/tickets/{ref}", ticket.getTicketReference()).header("Authorization", bearer(c.token())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/tickets/{ref}", ticket.getTicketReference()).header("Authorization", bearer(staff)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/customer/tickets")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/customer/tickets").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }

    private Context context(String email, int seatCount, BookingStatus status) throws Exception {
        User user = saveUser(email, Role.CUSTOMER);
        String token = loginToken(email);
        String admin = tokenFor("admin-" + email, Role.ADMIN);
        var show = saveShowWithSeats(saveMovie("Movie " + email, com.chalchitraghar.modules.movies.enums.MovieStatus.NOW_SHOWING),
                saveHall("Hall " + email, com.chalchitraghar.modules.halls.enums.Status.ACTIVE), admin);
        var selected = seatsForShow(show.getId()).subList(0, seatCount);
        selected.forEach(s -> s.setSeatStatus(status == BookingStatus.CONFIRMED ? SeatStatus.BOOKED : SeatStatus.RESERVED));
        seatRepository.saveAll(selected);
        BigDecimal total = selected.stream().map(s -> s.getPrice()).reduce(BigDecimal.ZERO, BigDecimal::add);
        Booking booking = bookingRepository.save(Booking.builder().user(user).show(show)
                .bookingReference("HCG-20260712-" + Math.abs(email.hashCode())).bookingTime(LocalDateTime.now(clock))
                .status(status).totalAmount(total).currency("NPR").confirmedAt(status == BookingStatus.CONFIRMED ? LocalDateTime.now(clock) : null)
                .expiresAt(LocalDateTime.now(clock).plusMinutes(15)).build());
        List<BookingSeat> claims = selected.stream().map(s -> BookingSeat.builder().booking(booking).seat(s).unitPrice(s.getPrice()).build()).toList();
        bookingSeatRepository.saveAll(claims);
        return new Context(user, token, booking);
    }
    private record Context(User user, String token, Booking booking) {}
}
