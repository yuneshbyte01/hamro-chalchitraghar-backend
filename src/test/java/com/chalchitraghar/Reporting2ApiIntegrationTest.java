package com.chalchitraghar;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.chalchitraghar.modules.bookings.entity.*;
import com.chalchitraghar.modules.bookings.enums.BookingStatus;
import com.chalchitraghar.modules.halls.enums.Status;
import com.chalchitraghar.modules.movies.enums.MovieStatus;
import com.chalchitraghar.modules.payments.entity.*;
import com.chalchitraghar.modules.payments.enums.*;
import com.chalchitraghar.modules.seats.entity.Seat;
import com.chalchitraghar.modules.seats.enums.*;
import com.chalchitraghar.modules.shows.entity.Show;
import com.chalchitraghar.modules.shows.enums.ShowStatus;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.modules.users.enums.Role;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class Reporting2ApiIntegrationTest extends AbstractIntegrationTest {
    private static final LocalDate START = LocalDate.of(2026, 7, 1);
    private static final LocalDate END = LocalDate.of(2026, 7, 3);

    @Test
    void emptyReportsReturnCompleteBucketsAndPagedZeroResults() throws Exception {
        String admin = tokenFor("report2-empty@example.com", Role.ADMIN);
        mockMvc.perform(report("/revenue", admin).param("groupBy", "DAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trends", hasSize(0)));
        mockMvc.perform(report("/revenue", admin).param("currency", "NPR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trends", hasSize(3)))
                .andExpect(jsonPath("$.data.trends[*].grossRevenue", everyItem(is(0.00))));
        mockMvc.perform(report("/bookings", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trends", hasSize(3)))
                .andExpect(jsonPath("$.data.trends[*].totalBookings", everyItem(is(0))));
        for (String endpoint : new String[] {"/occupancy", "/movies", "/halls", "/shows"}) {
            mockMvc.perform(report(endpoint, admin)).andExpect(status().isOk());
        }
    }

    @Test
    void rowMultiplicationScenarioKeepsSeatsPaymentsAndRefundsIndependent() throws Exception {
        String admin = tokenFor("report2-multiply@example.com", Role.ADMIN);
        User customer = saveUser("report2-owner@example.com", Role.CUSTOMER);
        var movie = saveMovie("Reporting 2 Movie", MovieStatus.NOW_SHOWING);
        var hall = saveHall("Reporting 2 Hall", Status.ACTIVE);
        Show show =
                Show.builder()
                        .movie(movie)
                        .hall(hall)
                        .status(ShowStatus.SCHEDULED)
                        .showDate(START.plusDays(1))
                        .showTime(LocalTime.NOON)
                        .endTime(LocalTime.of(14, 0))
                        .build();
        show = showRepository.save(show);
        show.setStatus(ShowStatus.SCHEDULED);
        show = showRepository.save(show);
        Seat first = seat(show, "A1", 1);
        Seat second = seat(show, "A2", 2);
        Booking booking =
                Booking.builder()
                        .bookingReference("HCG-R2-" + UUID.randomUUID())
                        .user(customer)
                        .show(show)
                        .bookingTime(START.atTime(10, 0))
                        .status(BookingStatus.CONFIRMED)
                        .totalAmount(new BigDecimal("200.00"))
                        .currency("NPR")
                        .build();
        booking = bookingRepository.save(booking);
        bookingSeatRepository.save(
                BookingSeat.builder()
                        .booking(booking)
                        .seat(first)
                        .unitPrice(new BigDecimal("100.00"))
                        .build());
        bookingSeatRepository.save(
                BookingSeat.builder()
                        .booking(booking)
                        .seat(second)
                        .unitPrice(new BigDecimal("100.00"))
                        .build());
        Payment one = payment(booking, "100.00", PaymentStatus.SUCCESS);
        payment(booking, "50.00", PaymentStatus.SUCCESS);
        Refund refund =
                Refund.builder()
                        .payment(one)
                        .booking(booking)
                        .refundReference("RFD-R2-" + UUID.randomUUID())
                        .amount(new BigDecimal("25.00"))
                        .currency("NPR")
                        .status(RefundStatus.SUCCEEDED)
                        .reason(RefundReason.OTHER)
                        .type(RefundType.FULL)
                        .method(RefundMethod.MANUAL)
                        .idempotencyKey("r2-" + UUID.randomUUID())
                        .requestedAt(START.atStartOfDay())
                        .processedAt(START.atTime(12, 0))
                        .completedAt(START.atTime(12, 0))
                        .build();
        refund = refundRepository.save(refund);
        for (int attempt = 1; attempt <= 2; attempt++) {
            refundAttemptRepository.save(
                    RefundAttempt.builder()
                            .refund(refund)
                            .attemptNumber(attempt)
                            .method(RefundMethod.MANUAL)
                            .status(
                                    attempt == 1
                                            ? RefundAttemptStatus.FAILED
                                            : RefundAttemptStatus.SUCCEEDED)
                            .startedAt(START.atTime(11, attempt))
                            .completedAt(START.atTime(11, attempt + 1))
                            .build());
        }

        mockMvc.perform(report("/shows", admin).param("currency", "NPR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].ticketsSold").value(2))
                .andExpect(jsonPath("$.data.content[0].confirmedBookingCount").value(1))
                .andExpect(
                        jsonPath("$.data.content[0].revenueByCurrency[0].grossRevenue")
                                .value(150.00))
                .andExpect(
                        jsonPath("$.data.content[0].revenueByCurrency[0].refundAmount")
                                .value(25.00))
                .andExpect(
                        jsonPath("$.data.content[0].revenueByCurrency[0].netRevenue")
                                .value(125.00));
        mockMvc.perform(report("/occupancy", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalGeneratedSeats").value(2))
                .andExpect(jsonPath("$.data.totalSoldSeats").value(2))
                .andExpect(jsonPath("$.data.overallOccupancyPercentage").value(100.00));
    }

    @Test
    void groupingValidationPaginationAndAuthorizationAreEnforced() throws Exception {
        String admin = tokenFor("report2-validation@example.com", Role.ADMIN);
        String customer = tokenFor("report2-customer@example.com", Role.CUSTOMER);
        mockMvc.perform(report("/revenue", admin).param("groupBy", "YEAR"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(report("/occupancy", admin).param("page", "-1"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(report("/movies", admin).param("size", "101"))
                .andExpect(status().isBadRequest());
        for (String endpoint :
                new String[] {
                    "/revenue", "/bookings", "/occupancy", "/movies", "/halls", "/shows"
                }) {
            mockMvc.perform(report(endpoint, null)).andExpect(status().isUnauthorized());
            mockMvc.perform(report(endpoint, customer)).andExpect(status().isForbidden());
            mockMvc.perform(report(endpoint, admin)).andExpect(status().isOk());
        }
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder report(
            String endpoint, String token) {
        var request =
                get("/api/admin/reports" + endpoint)
                        .param("startDate", START.toString())
                        .param("endDate", END.toString());
        return token == null ? request : request.header("Authorization", bearer(token));
    }

    private Seat seat(Show show, String code, int position) {
        return seatRepository.save(
                Seat.builder()
                        .show(show)
                        .seatNumber(position)
                        .rowLabel("A")
                        .seatCode(code)
                        .seatType(SeatType.PREMIUM)
                        .price(new BigDecimal("100.00"))
                        .seatStatus(SeatStatus.BOOKED)
                        .positionIndex(position)
                        .build());
    }

    private Payment payment(Booking booking, String amount, PaymentStatus status) {
        String reference = "PAY-R2-" + UUID.randomUUID();
        return paymentRepository.save(
                Payment.builder()
                        .booking(booking)
                        .paymentReference(reference)
                        .provider(PaymentProvider.LOCAL)
                        .method(PaymentMethod.ONLINE)
                        .status(status)
                        .amount(new BigDecimal(amount))
                        .currency("NPR")
                        .providerTransactionId(reference)
                        .initiatedAt(START.atTime(9, 0))
                        .completedAt(START.atTime(9, 30))
                        .build());
    }
}
