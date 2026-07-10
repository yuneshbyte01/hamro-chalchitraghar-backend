package com.chalchitraghar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import com.chalchitraghar.modules.bookings.entity.Booking;
import com.chalchitraghar.modules.bookings.enums.BookingStatus;
import com.chalchitraghar.modules.halls.entity.Hall;
import com.chalchitraghar.modules.halls.enums.Status;
import com.chalchitraghar.modules.movies.entity.Movie;
import com.chalchitraghar.modules.movies.enums.MovieStatus;
import com.chalchitraghar.modules.seats.entity.Seat;
import com.chalchitraghar.modules.seats.enums.SeatStatus;
import com.chalchitraghar.modules.shows.entity.Show;
import com.chalchitraghar.modules.shows.enums.ShowStatus;
import com.chalchitraghar.modules.users.enums.Role;
import com.fasterxml.jackson.databind.JsonNode;

class BookingApiIntegrationTest extends AbstractIntegrationTest {

    @Test
    void customerCanHoldAvailableSeats() throws Exception {
        TestShowContext context = createShowContext("hold@example.com");
        Seat seat = seatsForShow(context.show().getId()).get(0);

        mockMvc.perform(post("/api/customer/bookings/hold")
                        .header("Authorization", bearer(context.customerToken()))
                        .contentType("application/json")
                        .content(json(Map.of("showId", context.show().getId(), "seatIds", List.of(seat.getId())))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Seats held successfully"))
                .andExpect(jsonPath("$.data.heldSeatCount").value(1))
                .andExpect(jsonPath("$.errors").isArray());

        Seat lockedSeat = seatRepository.findById(seat.getId()).orElseThrow();
        assertThat(lockedSeat.getSeatStatus()).isEqualTo(SeatStatus.LOCKED);
    }

    @Test
    void sameCustomerCanCreateBookingFromHeldSeats() throws Exception {
        TestShowContext context = createShowContext("held-booking@example.com");
        Seat seat = seatsForShow(context.show().getId()).get(0);
        holdSeat(context.customerToken(), context.show().getId(), seat.getId());

        mockMvc.perform(post("/api/customer/bookings")
                        .header("Authorization", bearer(context.customerToken()))
                        .contentType("application/json")
                        .content(json(Map.of("showId", context.show().getId(), "seatIds", List.of(seat.getId())))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Booking created successfully"))
                .andExpect(jsonPath("$.data.bookingStatus").value("INITIATED"))
                .andExpect(jsonPath("$.errors").isArray());

        assertThat(seatRepository.findById(seat.getId()).orElseThrow().getSeatStatus()).isEqualTo(SeatStatus.RESERVED);
    }

    @Test
    void anotherCustomerCannotBookSeatsHeldBySomeoneElse() throws Exception {
        TestShowContext context = createShowContext("holder@example.com");
        String otherCustomerToken = tokenFor("other-booker@example.com", Role.CUSTOMER);
        Seat seat = seatsForShow(context.show().getId()).get(0);
        holdSeat(context.customerToken(), context.show().getId(), seat.getId());

        mockMvc.perform(post("/api/customer/bookings")
                        .header("Authorization", bearer(otherCustomerToken))
                        .contentType("application/json")
                        .content(json(Map.of("showId", context.show().getId(), "seatIds", List.of(seat.getId())))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void customerCanCreateBookingDirectlyFromAvailableSeats() throws Exception {
        TestShowContext context = createShowContext("direct-booking@example.com");
        Seat seat = seatsForShow(context.show().getId()).get(0);

        mockMvc.perform(post("/api/customer/bookings")
                        .header("Authorization", bearer(context.customerToken()))
                        .contentType("application/json")
                        .content(json(Map.of("showId", context.show().getId(), "seatIds", List.of(seat.getId())))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bookingStatus").value("INITIATED"));

        assertThat(seatRepository.findById(seat.getId()).orElseThrow().getSeatStatus()).isEqualTo(SeatStatus.RESERVED);
    }

    @Test
    void confirmBookingMarksBookingConfirmedAndSeatsBooked() throws Exception {
        TestShowContext context = createShowContext("confirm-booking@example.com");
        Seat seat = seatsForShow(context.show().getId()).get(0);
        Long bookingId = createBooking(context.customerToken(), context.show().getId(), seat.getId());

        mockMvc.perform(post("/api/customer/bookings/{bookingId}/confirm", bookingId)
                        .header("Authorization", bearer(context.customerToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Booking confirmed successfully"))
                .andExpect(jsonPath("$.data.bookingStatus").value("CONFIRMED"));

        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(seatRepository.findById(seat.getId()).orElseThrow().getSeatStatus()).isEqualTo(SeatStatus.BOOKED);
    }

    @Test
    void cancelInitiatedBookingMarksBookingCancelledAndSeatsAvailable() throws Exception {
        TestShowContext context = createShowContext("cancel-booking@example.com");
        Seat seat = seatsForShow(context.show().getId()).get(0);
        Long bookingId = createBooking(context.customerToken(), context.show().getId(), seat.getId());

        mockMvc.perform(post("/api/customer/bookings/{bookingId}/cancel", bookingId)
                        .header("Authorization", bearer(context.customerToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Booking cancelled successfully"))
                .andExpect(jsonPath("$.data.bookingStatus").value("CANCELLED"));

        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(seatRepository.findById(seat.getId()).orElseThrow().getSeatStatus()).isEqualTo(SeatStatus.AVAILABLE);
    }

    @Test
    void expiredLockedSeatsCanBeHeldAgain() throws Exception {
        TestShowContext context = createShowContext("expired-lock@example.com");
        Seat seat = seatsForShow(context.show().getId()).get(0);
        expireLock(seat);

        mockMvc.perform(post("/api/customer/bookings/hold")
                        .header("Authorization", bearer(context.customerToken()))
                        .contentType("application/json")
                        .content(json(Map.of("showId", context.show().getId(), "seatIds", List.of(seat.getId())))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.heldSeatCount").value(1));

        Seat relockedSeat = seatRepository.findById(seat.getId()).orElseThrow();
        assertThat(relockedSeat.getSeatStatus()).isEqualTo(SeatStatus.LOCKED);
    }

    @Test
    void duplicateSeatIdsAreRejected() throws Exception {
        TestShowContext context = createShowContext("duplicate-seats@example.com");
        Seat seat = seatsForShow(context.show().getId()).get(0);

        mockMvc.perform(post("/api/customer/bookings/hold")
                        .header("Authorization", bearer(context.customerToken()))
                        .contentType("application/json")
                        .content(json(Map.of("showId", context.show().getId(), "seatIds", List.of(seat.getId(), seat.getId())))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Seat IDs contain duplicates"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void holdsAndBookingsRejectCancelledOrCompletedShows() throws Exception {
        TestShowContext context = createShowContext("unavailable-show@example.com");
        Seat seat = seatsForShow(context.show().getId()).getFirst();
        context.show().setStatus(ShowStatus.CANCELLED);
        showRepository.save(context.show());

        mockMvc.perform(post("/api/customer/bookings/hold")
                        .header("Authorization", bearer(context.customerToken()))
                        .contentType("application/json")
                        .content(json(Map.of("showId", context.show().getId(), "seatIds", List.of(seat.getId())))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Seats cannot be held for a cancelled or completed show"));
        mockMvc.perform(post("/api/customer/bookings")
                        .header("Authorization", bearer(context.customerToken()))
                        .contentType("application/json")
                        .content(json(Map.of("showId", context.show().getId(), "seatIds", List.of(seat.getId())))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Booking is not allowed for a cancelled or completed show"));

        context.show().setStatus(ShowStatus.COMPLETED);
        showRepository.save(context.show());
        mockMvc.perform(post("/api/customer/bookings/hold")
                        .header("Authorization", bearer(context.customerToken()))
                        .contentType("application/json")
                        .content(json(Map.of("showId", context.show().getId(), "seatIds", List.of(seat.getId())))))
                .andExpect(status().isConflict());
    }

    private TestShowContext createShowContext(String customerEmail) throws Exception {
        String adminToken = tokenFor("admin-" + customerEmail, Role.ADMIN);
        String customerToken = tokenFor(customerEmail, Role.CUSTOMER);
        Movie movie = saveMovie("Booking Movie " + customerEmail, MovieStatus.NOW_SHOWING);
        Hall hall = saveHall("Booking Hall " + customerEmail, Status.ACTIVE);
        Show show = saveShowWithSeats(movie, hall, adminToken);
        return new TestShowContext(customerToken, show);
    }

    private void holdSeat(String token, Long showId, Long seatId) throws Exception {
        mockMvc.perform(post("/api/customer/bookings/hold")
                        .header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content(json(Map.of("showId", showId, "seatIds", List.of(seatId)))))
                .andExpect(status().isOk());
    }

    private Long createBooking(String token, Long showId, Long seatId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/customer/bookings")
                        .header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content(json(Map.of("showId", showId, "seatIds", List.of(seatId)))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.path("data").path("bookingId").asLong();
    }

    private record TestShowContext(String customerToken, Show show) {
    }
}
