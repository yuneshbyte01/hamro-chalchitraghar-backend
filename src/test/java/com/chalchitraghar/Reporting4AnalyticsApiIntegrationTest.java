package com.chalchitraghar;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.chalchitraghar.modules.bookings.entity.Booking;
import com.chalchitraghar.modules.bookings.enums.BookingStatus;
import com.chalchitraghar.modules.halls.enums.Status;
import com.chalchitraghar.modules.movies.enums.MovieStatus;
import com.chalchitraghar.modules.payments.entity.Payment;
import com.chalchitraghar.modules.payments.enums.*;
import com.chalchitraghar.modules.shows.entity.Show;
import com.chalchitraghar.modules.shows.enums.ShowStatus;
import com.chalchitraghar.modules.users.enums.Role;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class Reporting4AnalyticsApiIntegrationTest extends AbstractIntegrationTest {
    private static final LocalDate START = LocalDate.of(2026, 7, 1), END = LocalDate.of(2026, 7, 3);

    @Test
    void emptyAnalyticsReturnCompleteBucketsAndNoPii() throws Exception {
        String admin = tokenFor("report4-empty-admin@example.com", Role.ADMIN);
        mockMvc.perform(report("/booking-patterns", admin).param("groupBy", "HOUR_OF_DAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(24)));
        mockMvc.perform(report("/booking-patterns", admin).param("groupBy", "DAY_OF_WEEK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(7)))
                .andExpect(jsonPath("$.data[0].label").value("MONDAY"));
        mockMvc.perform(report("/show-times", admin).param("groupBy", "TIME_SLOT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(4)));
        mockMvc.perform(report("/show-times", admin).param("groupBy", "HOUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(24)));
        for (String endpoint :
                new String[] {
                    "/overview",
                    "/seats",
                    "/customers",
                    "/conversion",
                    "/refunds",
                    "/performance-extremes"
                })
            mockMvc.perform(report(endpoint, admin))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("email"))));
        mockMvc.perform(
                        get("/api/admin/reports/analytics/data-quality")
                                .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalWarnings").value(0));
        mockMvc.perform(get("/v3/api-docs").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/admin/reports/analytics/overview']").exists())
                .andExpect(jsonPath("$.paths['/api/admin/reports/deliveries']").exists());
        mockMvc.perform(report("/revenue-concentration", admin).param("currency", "NPR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRecognizedGrossRevenue").value(0.00));
    }

    @Test
    void bookingCustomerAndPaymentConversionDefinitionsAreApplied() throws Exception {
        String admin = tokenFor("report4-metrics-admin@example.com", Role.ADMIN);
        var customer = saveUser("report4-customer@example.com", Role.CUSTOMER);
        var movie = saveMovie("Reporting 4 Movie", MovieStatus.NOW_SHOWING);
        var hall = saveHall("Reporting 4 Hall", Status.ACTIVE);
        Show show =
                showRepository.save(
                        Show.builder()
                                .movie(movie)
                                .hall(hall)
                                .showDate(START)
                                .showTime(LocalTime.of(18, 0))
                                .endTime(LocalTime.of(20, 0))
                                .status(ShowStatus.SCHEDULED)
                                .build());
        Booking first = booking(customer, show, START.atTime(9, 0), BookingStatus.CONFIRMED);
        booking(customer, show, START.atTime(10, 0), BookingStatus.CONFIRMED);
        booking(customer, show, START.atTime(11, 0), BookingStatus.CANCELLED);
        payment(first, PaymentStatus.REFUNDED);
        payment(first, PaymentStatus.FAILED);
        payment(first, PaymentStatus.PENDING);
        mockMvc.perform(report("/customers", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uniqueCustomersWithConfirmedBooking").value(1))
                .andExpect(jsonPath("$.data.repeatConfirmedCustomers").value(1))
                .andExpect(jsonPath("$.data.repeatCustomerRate").value(100.00));
        mockMvc.perform(report("/conversion", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalBookingAttempts").value(3))
                .andExpect(jsonPath("$.data.bookingAttemptConversionRate").value(66.67))
                .andExpect(jsonPath("$.data.terminalPaymentAttempts").value(2))
                .andExpect(jsonPath("$.data.paymentAttemptSuccessRate").value(50.00));
        mockMvc.perform(report("/booking-patterns", admin).param("groupBy", "HOUR_OF_DAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[9].bookingCount").value(1));
    }

    @Test
    void analyticsAreAdminOnlyAndValidateCurrencyAndLimits() throws Exception {
        String customer = tokenFor("report4-security-customer@example.com", Role.CUSTOMER),
                admin = tokenFor("report4-security-admin@example.com", Role.ADMIN);
        mockMvc.perform(report("/overview", null)).andExpect(status().isUnauthorized());
        mockMvc.perform(report("/overview", customer)).andExpect(status().isForbidden());
        mockMvc.perform(report("/revenue-concentration", admin)).andExpect(status().isBadRequest());
        mockMvc.perform(
                        report("/revenue-concentration", admin)
                                .param("currency", "NPR")
                                .param("top", "21"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(report("/performance-extremes", admin).param("minimumShows", "0"))
                .andExpect(status().isBadRequest());
    }

    private Booking booking(
            com.chalchitraghar.modules.users.entity.User user,
            Show show,
            LocalDateTime time,
            BookingStatus status) {
        return bookingRepository.save(
                Booking.builder()
                        .bookingReference("HCG-R4-" + UUID.randomUUID())
                        .user(user)
                        .show(show)
                        .bookingTime(time)
                        .status(status)
                        .totalAmount(new BigDecimal("100.00"))
                        .currency("NPR")
                        .build());
    }

    private void payment(Booking booking, PaymentStatus status) {
        String ref = "PAY-R4-" + UUID.randomUUID();
        paymentRepository.save(
                Payment.builder()
                        .booking(booking)
                        .paymentReference(ref)
                        .provider(PaymentProvider.LOCAL)
                        .method(PaymentMethod.ONLINE)
                        .status(status)
                        .amount(new BigDecimal("100.00"))
                        .currency("NPR")
                        .providerTransactionId(ref)
                        .initiatedAt(START.atTime(9, 0))
                        .completedAt(status == PaymentStatus.PENDING ? null : START.atTime(9, 30))
                        .failedAt(status == PaymentStatus.FAILED ? START.atTime(9, 30) : null)
                        .build());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder report(
            String endpoint, String token) {
        var request =
                get("/api/admin/reports/analytics" + endpoint)
                        .param("startDate", START.toString())
                        .param("endDate", END.toString());
        return token == null ? request : request.header("Authorization", bearer(token));
    }
}
