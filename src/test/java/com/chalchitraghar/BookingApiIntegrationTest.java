package com.chalchitraghar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.beans.factory.annotation.Autowired;

import com.chalchitraghar.modules.bookings.entity.Booking;
import com.chalchitraghar.modules.bookings.enums.BookingStatus;
import com.chalchitraghar.modules.bookings.enums.ConfirmationSource;
import com.chalchitraghar.modules.bookings.service.ExpiredBookingCleanupJob;
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

    @Autowired
    private ExpiredBookingCleanupJob expiredBookingCleanupJob;

    @Test
    void scheduledCleanupExpiresOnlyDueInitiatedBookingsAndIsRepeatable() throws Exception {
        TestShowContext expiredContext = createShowContext("scheduled-expired@example.com");
        Seat expiredSeat = seatsForShow(expiredContext.show().getId()).getFirst();
        Long expiredId = createBooking(expiredContext.customerToken(), expiredContext.show().getId(), expiredSeat.getId());
        Booking due = bookingRepository.findById(expiredId).orElseThrow();
        due.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        bookingRepository.save(due);

        TestShowContext validContext = createShowContext("scheduled-valid@example.com");
        Long validId = createBooking(validContext.customerToken(), validContext.show().getId(),
                seatsForShow(validContext.show().getId()).getFirst().getId());

        assertThat(expiredBookingCleanupJob.expireBatch()).isEqualTo(1);
        assertThat(expiredBookingCleanupJob.expireBatch()).isZero();
        assertThat(bookingRepository.findById(expiredId).orElseThrow().getStatus()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(bookingRepository.findById(validId).orElseThrow().getStatus()).isEqualTo(BookingStatus.INITIATED);
        assertThat(seatRepository.findById(expiredSeat.getId()).orElseThrow().getSeatStatus())
                .isEqualTo(SeatStatus.AVAILABLE);
    }

    @Test
    void concurrentCustomersCannotCreateTwoBookingsForTheSameSeat() throws Exception {
        TestShowContext context = createShowContext("concurrent-owner@example.com");
        String otherToken = tokenFor("concurrent-other@example.com", Role.CUSTOMER);
        Seat seat = seatsForShow(context.show().getId()).getFirst();
        String body = json(Map.of("showId", context.show().getId(), "seatIds", List.of(seat.getId())));
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> {
                start.await();
                return mockMvc.perform(post("/api/customer/bookings").header("Authorization", bearer(context.customerToken()))
                        .contentType("application/json").content(body)).andReturn().getResponse().getStatus();
            });
            var second = executor.submit(() -> {
                start.await();
                return mockMvc.perform(post("/api/customer/bookings").header("Authorization", bearer(otherToken))
                        .contentType("application/json").content(body)).andReturn().getResponse().getStatus();
            });
            start.countDown();
            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(201, 409);
        }
        assertThat(bookingSeatRepository.findBySeatIds(List.of(seat.getId()))).hasSize(1);
        assertThat(seatRepository.findById(seat.getId()).orElseThrow().getSeatStatus()).isEqualTo(SeatStatus.RESERVED);
    }

    @Test
    void bookingReferenceSupportsOwnedAndManagementLookups() throws Exception {
        TestShowContext context = createShowContext("reference-owner@example.com");
        Long bookingId = createBooking(context.customerToken(), context.show().getId(),
                seatsForShow(context.show().getId()).getFirst().getId());
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(booking.getBookingReference()).matches("HCG-\\d{8}-[A-Z0-9]{8}");
        assertThat(booking.getExpiresAt()).isAfter(booking.getBookingTime());
        String otherToken = tokenFor("reference-other@example.com", Role.CUSTOMER);
        String staffToken = tokenFor("reference-staff@example.com", Role.STAFF);
        String adminToken = tokenFor("reference-admin@example.com", Role.ADMIN);

        mockMvc.perform(get("/api/customer/bookings/reference/{reference}", booking.getBookingReference())
                        .header("Authorization", bearer(context.customerToken())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.bookingReference").value(booking.getBookingReference()))
                .andExpect(jsonPath("$.data.currency").value("NPR"));
        mockMvc.perform(get("/api/customer/bookings/reference/{reference}", booking.getBookingReference())
                        .header("Authorization", bearer(otherToken))).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/staff/bookings/reference/{reference}", booking.getBookingReference())
                        .header("Authorization", bearer(staffToken))).andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/bookings/reference/{reference}", booking.getBookingReference())
                        .header("Authorization", bearer(adminToken))).andExpect(status().isOk());
        mockMvc.perform(get("/api/customer/bookings/reference/UNKNOWN")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/bookings/reference/UNKNOWN")
                        .header("Authorization", bearer(adminToken))).andExpect(status().isNotFound());
    }

    @Test
    void bookingPricingIsAnImmutableExactSnapshot() throws Exception {
        TestShowContext context = createShowContext("price-snapshot@example.com");
        List<Seat> seats = seatsForShow(context.show().getId());
        Long bookingId = createBooking(context.customerToken(), context.show().getId(), seats.getFirst().getId());
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        var bookingSeats = bookingSeatRepository.findByBookingId(bookingId);
        assertThat(booking.getTotalAmount()).isEqualByComparingTo(bookingSeats.getFirst().getUnitPrice());
        assertThat(booking.getCurrency()).isEqualTo("NPR");

        Seat seat = seatRepository.findById(seats.getFirst().getId()).orElseThrow();
        seat.setPrice(new BigDecimal("9999.99"));
        seatRepository.save(seat);
        mockMvc.perform(get("/api/customer/bookings/{id}", bookingId)
                        .header("Authorization", bearer(context.customerToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(booking.getTotalAmount().doubleValue()))
                .andExpect(jsonPath("$.data.selectedSeats[0].price").value(bookingSeats.getFirst().getUnitPrice().doubleValue()));
    }

    @Test
    void staleInitiatedBookingExpiresLazilyAndReleasesReservedSeats() throws Exception {
        TestShowContext context = createShowContext("lazy-expiry@example.com");
        Seat seat = seatsForShow(context.show().getId()).getFirst();
        Long bookingId = createBooking(context.customerToken(), context.show().getId(), seat.getId());
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        booking.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        bookingRepository.save(booking);

        mockMvc.perform(get("/api/customer/bookings/{id}", bookingId)
                        .header("Authorization", bearer(context.customerToken())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.bookingStatus").value("EXPIRED"));
        Booking expired = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(expired.getExpiredAt()).isNotNull();
        assertThat(seatRepository.findById(seat.getId()).orElseThrow().getSeatStatus()).isEqualTo(SeatStatus.AVAILABLE);
        mockMvc.perform(post("/api/customer/bookings/{id}/confirm", bookingId)
                        .header("Authorization", bearer(context.customerToken()))).andExpect(status().isConflict());
        mockMvc.perform(post("/api/customer/bookings/{id}/cancel", bookingId)
                        .header("Authorization", bearer(context.customerToken()))).andExpect(status().isConflict());
    }

    @Test
    void everyCustomerBookingEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/customer/bookings/hold").contentType("application/json")
                        .content(json(Map.of("showId", 1, "seatIds", List.of(1)))))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/customer/bookings").contentType("application/json")
                        .content(json(Map.of("showId", 1, "seatIds", List.of(1)))))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/customer/bookings/1/confirm")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/customer/bookings/my")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/customer/bookings/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/customer/bookings/1/cancel")).andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTokenCannotAccessCustomerBookings() throws Exception {
        mockMvc.perform(get("/api/customer/bookings/my").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerDetailIsOwnerOnlyAndDoesNotExposeManagementFields() throws Exception {
        TestShowContext context = createShowContext("detail-owner@example.com");
        Long bookingId = createBooking(context.customerToken(), context.show().getId(),
                seatsForShow(context.show().getId()).getFirst().getId());
        String otherToken = tokenFor("detail-other@example.com", Role.CUSTOMER);

        mockMvc.perform(get("/api/customer/bookings/{bookingId}", bookingId)
                        .header("Authorization", bearer(context.customerToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customerId").doesNotExist())
                .andExpect(jsonPath("$.data.customerEmail").doesNotExist())
                .andExpect(jsonPath("$.data.createdAt").doesNotExist())
                .andExpect(jsonPath("$.data.updatedAt").doesNotExist())
                .andExpect(jsonPath("$.data.selectedSeats[0].lockedByUserId").doesNotExist());
        mockMvc.perform(get("/api/customer/bookings/{bookingId}", bookingId)
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/customer/bookings/{bookingId}/confirm", bookingId)
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/customer/bookings/{bookingId}/cancel", bookingId)
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerHistoryUsesSummaryContractAndDeterministicSeatOrdering() throws Exception {
        TestShowContext context = createShowContext("summary@example.com");
        List<Seat> seats = seatsForShow(context.show().getId());
        mockMvc.perform(post("/api/customer/bookings")
                        .header("Authorization", bearer(context.customerToken())).contentType("application/json")
                        .content(json(Map.of("showId", context.show().getId(),
                                "seatIds", List.of(seats.get(1).getId(), seats.get(0).getId())))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.selectedSeats[0].positionIndex").value(0))
                .andExpect(jsonPath("$.data.selectedSeats[1].positionIndex").value(1));

        mockMvc.perform(get("/api/customer/bookings/my").header("Authorization", bearer(context.customerToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].selectedSeatCodes[0]").value(seats.get(0).getSeatCode()))
                .andExpect(jsonPath("$.data.content[0].selectedSeats").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].customerEmail").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void staffAndAdminUseSeparateManagementEndpoints() throws Exception {
        TestShowContext context = createShowContext("management-detail@example.com");
        Long bookingId = createBooking(context.customerToken(), context.show().getId(),
                seatsForShow(context.show().getId()).getFirst().getId());
        String staffToken = tokenFor("booking-staff@example.com", Role.STAFF);
        String adminToken = tokenFor("booking-admin@example.com", Role.ADMIN);
        String customerToken = tokenFor("booking-customer@example.com", Role.CUSTOMER);

        mockMvc.perform(get("/api/staff/bookings/{bookingId}", bookingId)
                        .header("Authorization", bearer(staffToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customerEmail").value("management-detail@example.com"))
                .andExpect(jsonPath("$.data.createdAt").exists());
        mockMvc.perform(get("/api/admin/bookings/{bookingId}", bookingId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customerEmail").value("management-detail@example.com"))
                .andExpect(jsonPath("$.data.updatedAt").exists());

        mockMvc.perform(get("/api/staff/bookings/{bookingId}", bookingId)
                        .header("Authorization", bearer(customerToken))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/bookings/{bookingId}", bookingId)
                        .header("Authorization", bearer(customerToken))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/bookings/{bookingId}", bookingId)
                        .header("Authorization", bearer(staffToken))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/staff/bookings/{bookingId}", bookingId)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/bookings/{bookingId}", bookingId)).andExpect(status().isUnauthorized());
    }

    @Test
    void customerHistorySupportsPaginationStatusAndDateFiltersWithoutLeakingOtherUsers() throws Exception {
        TestShowContext owner = createShowContext("history-owner@example.com");
        Long ownerBooking = createBooking(owner.customerToken(), owner.show().getId(),
                seatsForShow(owner.show().getId()).getFirst().getId());
        TestShowContext other = createShowContext("history-other@example.com");
        createBooking(other.customerToken(), other.show().getId(), seatsForShow(other.show().getId()).getFirst().getId());

        mockMvc.perform(get("/api/customer/bookings/my")
                        .header("Authorization", bearer(owner.customerToken()))
                        .param("page", "0").param("size", "1")
                        .param("status", "INITIATED")
                        .param("showDateFrom", owner.show().getShowDate().toString())
                        .param("showDateTo", owner.show().getShowDate().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].bookingId").value(ownerBooking))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(1));
    }

    @Test
    void managementListsSupportSearchFiltersSortingAndSafeResponses() throws Exception {
        TestShowContext context = createShowContext("aarav.management@example.com");
        Long bookingId = createBooking(context.customerToken(), context.show().getId(),
                seatsForShow(context.show().getId()).getFirst().getId());
        String staffToken = tokenFor("list-staff@example.com", Role.STAFF);
        String adminToken = tokenFor("list-admin@example.com", Role.ADMIN);

        mockMvc.perform(get("/api/staff/bookings").header("Authorization", bearer(staffToken))
                        .param("search", "AARAV").param("status", "INITIATED")
                        .param("showId", context.show().getId().toString())
                        .param("movieId", context.show().getMovie().getId().toString())
                        .param("hallId", context.show().getHall().getId().toString())
                        .param("sortBy", "bookingTime").param("sortDir", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].bookingId").value(bookingId))
                .andExpect(jsonPath("$.data.content[0].customerEmail").value("aarav.management@example.com"))
                .andExpect(jsonPath("$.data.content[0].password").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].googleId").doesNotExist());

        mockMvc.perform(get("/api/admin/bookings").header("Authorization", bearer(adminToken))
                        .param("customerId", userRepository.findByEmail("aarav.management@example.com")
                                .orElseThrow().getId().toString())
                        .param("showDateFrom", context.show().getShowDate().toString())
                        .param("showDateTo", context.show().getShowDate().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void bookingListsRejectInvalidQueriesAndUnauthorizedRoles() throws Exception {
        String staffToken = tokenFor("query-staff@example.com", Role.STAFF);
        String customerToken = tokenFor("query-customer@example.com", Role.CUSTOMER);

        mockMvc.perform(get("/api/staff/bookings").header("Authorization", bearer(staffToken))
                        .param("status", "UNKNOWN")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/staff/bookings").header("Authorization", bearer(staffToken))
                        .param("sortBy", "user.password")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/staff/bookings").header("Authorization", bearer(staffToken))
                        .param("sortDir", "sideways")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/staff/bookings").header("Authorization", bearer(staffToken))
                        .param("showDateFrom", "2026-09-02").param("showDateTo", "2026-09-01"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/staff/bookings").header("Authorization", bearer(customerToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/bookings").header("Authorization", bearer(staffToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/staff/bookings")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/bookings")).andExpect(status().isUnauthorized());
    }

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
        assertThat(booking.getConfirmedAt()).isNotNull();
        assertThat(booking.getConfirmationSource()).isEqualTo(ConfirmationSource.CUSTOMER);
        assertThat(seatRepository.findById(seat.getId()).orElseThrow().getSeatStatus()).isEqualTo(SeatStatus.BOOKED);
        mockMvc.perform(post("/api/customer/bookings/{bookingId}/confirm", bookingId)
                        .header("Authorization", bearer(context.customerToken())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.bookingStatus").value("CONFIRMED"));
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
        assertThat(booking.getCancelledAt()).isNotNull();
        assertThat(seatRepository.findById(seat.getId()).orElseThrow().getSeatStatus()).isEqualTo(SeatStatus.AVAILABLE);
        mockMvc.perform(post("/api/customer/bookings/{bookingId}/cancel", bookingId)
                        .header("Authorization", bearer(context.customerToken())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.bookingStatus").value("CANCELLED"));
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
                .andExpect(jsonPath("$.message").value("Show is not bookable; it must be scheduled in the future with an active hall and a now-showing movie"));
        mockMvc.perform(post("/api/customer/bookings")
                        .header("Authorization", bearer(context.customerToken()))
                        .contentType("application/json")
                        .content(json(Map.of("showId", context.show().getId(), "seatIds", List.of(seat.getId())))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Show is not bookable; it must be scheduled in the future with an active hall and a now-showing movie"));

        context.show().setStatus(ShowStatus.COMPLETED);
        showRepository.save(context.show());
        mockMvc.perform(post("/api/customer/bookings/hold")
                        .header("Authorization", bearer(context.customerToken()))
                        .contentType("application/json")
                        .content(json(Map.of("showId", context.show().getId(), "seatIds", List.of(seat.getId())))))
                .andExpect(status().isConflict());
    }

    @Test
    void activeHoldBlocksUpdateButCancellationReleasesLockAndKeepsSeats() throws Exception {
        TestShowContext context = createShowContext("show-hold-dependency@example.com");
        String adminToken = loginToken("admin-show-hold-dependency@example.com");
        Seat seat = seatsForShow(context.show().getId()).getFirst();
        holdSeat(context.customerToken(), context.show().getId(), seat.getId());

        mockMvc.perform(put("/api/admin/shows/{id}", context.show().getId())
                        .header("Authorization", bearer(adminToken)).contentType("application/json")
                        .content(json(showRequest(context.show().getMovie().getId(), context.show().getHall().getId(),
                                11, "10:00", "12:00"))))
                .andExpect(status().isConflict());

        long seatCount = seatRepository.countByShowId(context.show().getId());
        mockMvc.perform(delete("/api/admin/shows/{id}", context.show().getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());
        Seat released = seatRepository.findById(seat.getId()).orElseThrow();
        assertThat(released.getSeatStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(released.getLockedAt()).isNull();
        assertThat(released.getLockExpiresAt()).isNull();
        assertThat(released.getLockedByUserId()).isNull();
        assertThat(seatRepository.countByShowId(context.show().getId())).isEqualTo(seatCount);

        mockMvc.perform(delete("/api/admin/shows/{id}", context.show().getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void showCancellationCancelsInitiatedBookingButConfirmedBookingStillBlocks() throws Exception {
        TestShowContext initiated = createShowContext("initiated-dependency@example.com");
        String initiatedAdmin = loginToken("admin-initiated-dependency@example.com");
        Seat initiatedSeat = seatsForShow(initiated.show().getId()).getFirst();
        Long bookingId = createBooking(initiated.customerToken(), initiated.show().getId(), initiatedSeat.getId());

        mockMvc.perform(put("/api/admin/shows/{id}", initiated.show().getId())
                        .header("Authorization", bearer(initiatedAdmin)).contentType("application/json")
                        .content(json(showRequest(initiated.show().getMovie().getId(), initiated.show().getHall().getId(),
                                11, "10:00", "12:00"))))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/admin/shows/{id}", initiated.show().getId())
                        .header("Authorization", bearer(initiatedAdmin)))
                .andExpect(status().isNoContent());
        assertThat(bookingRepository.findById(bookingId).orElseThrow().getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(bookingRepository.findById(bookingId).orElseThrow().getCancelledAt()).isNotNull();
        assertThat(seatRepository.findById(initiatedSeat.getId()).orElseThrow().getSeatStatus())
                .isEqualTo(SeatStatus.AVAILABLE);
        mockMvc.perform(get("/api/customer/bookings/{bookingId}", bookingId)
                        .header("Authorization", bearer(initiated.customerToken())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.bookingStatus").value("CANCELLED"));

        TestShowContext confirmed = createShowContext("confirmed-dependency@example.com");
        String confirmedAdmin = loginToken("admin-confirmed-dependency@example.com");
        Long confirmedId = createBooking(confirmed.customerToken(), confirmed.show().getId(),
                seatsForShow(confirmed.show().getId()).getFirst().getId());
        mockMvc.perform(post("/api/customer/bookings/{bookingId}/confirm", confirmedId)
                        .header("Authorization", bearer(confirmed.customerToken())))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/admin/shows/{id}", confirmed.show().getId())
                        .header("Authorization", bearer(confirmedAdmin)))
                .andExpect(status().isConflict());
    }

    @Test
    void holdsBookingsAndConfirmationCloseWhenShowHasStarted() throws Exception {
        TestShowContext context = createShowContext("started-show@example.com");
        Seat first = seatsForShow(context.show().getId()).get(0);
        Seat second = seatsForShow(context.show().getId()).get(1);
        Long bookingId = createBooking(context.customerToken(), context.show().getId(), first.getId());
        context.show().setShowDate(java.time.LocalDate.now());
        context.show().setShowTime(java.time.LocalTime.now().minusMinutes(30));
        context.show().setEndTime(java.time.LocalTime.now().plusMinutes(90));
        context.show().setStatus(ShowStatus.SCHEDULED);
        showRepository.save(context.show());

        mockMvc.perform(post("/api/customer/bookings/hold")
                        .header("Authorization", bearer(context.customerToken())).contentType("application/json")
                        .content(json(Map.of("showId", context.show().getId(), "seatIds", List.of(second.getId())))))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/customer/bookings")
                        .header("Authorization", bearer(context.customerToken())).contentType("application/json")
                        .content(json(Map.of("showId", context.show().getId(), "seatIds", List.of(second.getId())))))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/customer/bookings/{bookingId}/confirm", bookingId)
                        .header("Authorization", bearer(context.customerToken())))
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
