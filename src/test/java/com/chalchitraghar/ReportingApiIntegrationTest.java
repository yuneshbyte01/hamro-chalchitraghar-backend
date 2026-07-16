package com.chalchitraghar;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.chalchitraghar.modules.bookings.entity.Booking;
import com.chalchitraghar.modules.bookings.enums.BookingStatus;
import com.chalchitraghar.modules.halls.enums.Status;
import com.chalchitraghar.modules.movies.enums.MovieStatus;
import com.chalchitraghar.modules.payments.entity.*;
import com.chalchitraghar.modules.payments.enums.*;
import com.chalchitraghar.modules.shows.entity.Show;
import com.chalchitraghar.modules.shows.enums.ShowStatus;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.modules.users.enums.Role;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReportingApiIntegrationTest extends AbstractIntegrationTest {
    private static final LocalDate START = LocalDate.of(2026, 7, 1);
    private static final LocalDate END = LocalDate.of(2026, 7, 31);

    @Test
    void allReportingEndpointsAreAdminOnly() throws Exception {
        String customer = tokenFor("report-customer@example.com", Role.CUSTOMER);
        String staff = tokenFor("report-staff@example.com", Role.STAFF);
        String admin = tokenFor("report-admin@example.com", Role.ADMIN);
        for (String path : new String[] {"", "/bookings", "/revenue"}) {
            String url = "/api/admin/reports/dashboard" + path;
            mockMvc.perform(
                            get(url).param("startDate", START.toString())
                                    .param("endDate", END.toString()))
                    .andExpect(status().isUnauthorized());
            mockMvc.perform(
                            get(url).header("Authorization", bearer(customer))
                                    .param("startDate", START.toString())
                                    .param("endDate", END.toString()))
                    .andExpect(status().isForbidden());
            mockMvc.perform(
                            get(url).header("Authorization", bearer(staff))
                                    .param("startDate", START.toString())
                                    .param("endDate", END.toString()))
                    .andExpect(status().isForbidden());
            mockMvc.perform(
                            get(url).header("Authorization", bearer(admin))
                                    .param("startDate", START.toString())
                                    .param("endDate", END.toString()))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void dateAndCurrencyValidationUseTheStandardErrorResponse() throws Exception {
        String admin = tokenFor("report-validation@example.com", Role.ADMIN);
        String url = "/api/admin/reports/dashboard/revenue";
        mockMvc.perform(
                        get(url).header("Authorization", bearer(admin))
                                .param("endDate", END.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
        mockMvc.perform(
                        get(url).header("Authorization", bearer(admin))
                                .param("startDate", START.toString()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(
                        get(url).header("Authorization", bearer(admin))
                                .param("startDate", END.toString())
                                .param("endDate", START.toString()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(
                        get(url).header("Authorization", bearer(admin))
                                .param("startDate", "2025-01-01")
                                .param("endDate", "2026-01-02"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(
                        get(url).header("Authorization", bearer(admin))
                                .param("startDate", "not-a-date")
                                .param("endDate", END.toString()))
                .andExpect(status().isBadRequest());
        for (String currency : new String[] {"NP", "N1R"}) {
            mockMvc.perform(
                            get(url).header("Authorization", bearer(admin))
                                    .param("startDate", START.toString())
                                    .param("endDate", END.toString())
                                    .param("currency", currency))
                    .andExpect(status().isBadRequest());
        }
        mockMvc.perform(
                        get(url).header("Authorization", bearer(admin))
                                .param("startDate", "2026-07-16")
                                .param("endDate", "2026-07-16")
                                .param("currency", " usd "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currencies[0].currency").value("USD"))
                .andExpect(jsonPath("$.data.currencies[0].grossRevenue").value(0.00))
                .andExpect(
                        jsonPath("$.data.currencies[0].averageSuccessfulPaymentAmount")
                                .value(0.00));
    }

    @Test
    void bookingKpisIncludeEveryStatusAndRespectInclusiveApiDates() throws Exception {
        String admin = tokenFor("report-bookings@example.com", Role.ADMIN);
        User customer = saveUser("report-booking-owner@example.com", Role.CUSTOMER);
        Show show = show(ShowStatus.SCHEDULED);
        LocalDateTime start = START.atStartOfDay();
        saveBooking(customer, show, BookingStatus.CONFIRMED, start);
        saveBooking(
                customer,
                show,
                BookingStatus.CONFIRMED,
                END.plusDays(1).atStartOfDay().minusSeconds(1));
        saveBooking(customer, show, BookingStatus.CANCELLED, start.plusDays(2));
        saveBooking(customer, show, BookingStatus.EXPIRED, start.plusDays(3));
        saveBooking(customer, show, BookingStatus.INITIATED, start.plusDays(4));
        saveBooking(customer, show, BookingStatus.PENDING, start.minusSeconds(1));
        saveBooking(customer, show, BookingStatus.BOOKED, END.plusDays(1).atStartOfDay());

        mockMvc.perform(report("/bookings", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalBookings").value(5))
                .andExpect(jsonPath("$.data.confirmedBookings").value(2))
                .andExpect(jsonPath("$.data.cancelledBookings").value(1))
                .andExpect(jsonPath("$.data.expiredBookings").value(1))
                .andExpect(jsonPath("$.data.confirmationRate").value(40.00))
                .andExpect(jsonPath("$.data.cancellationRate").value(20.00))
                .andExpect(jsonPath("$.data.expirationRate").value(20.00))
                .andExpect(
                        jsonPath("$.data.statusBreakdown", hasSize(BookingStatus.values().length)))
                .andExpect(
                        jsonPath(
                                "$.data.statusBreakdown[?(@.status == 'INITIATED')].count",
                                contains(1)));
    }

    @Test
    void revenueKpisApplyFinancialDefinitionsAndNeverCombineCurrencies() throws Exception {
        String admin = tokenFor("report-revenue@example.com", Role.ADMIN);
        User customer = saveUser("report-revenue-owner@example.com", Role.CUSTOMER);
        Show show = show(ShowStatus.SCHEDULED);
        Booking booking =
                saveBooking(customer, show, BookingStatus.CONFIRMED, START.atStartOfDay());
        LocalDateTime within = START.atStartOfDay().plusHours(1);
        payment(booking, PaymentStatus.SUCCESS, "NPR", "100.00", within);
        Payment refunded =
                payment(booking, PaymentStatus.REFUNDED, "NPR", "50.00", within.plusHours(1));
        payment(booking, PaymentStatus.SUCCESS, "USD", "20.00", within);
        for (PaymentStatus status :
                new PaymentStatus[] {
                    PaymentStatus.CREATED,
                    PaymentStatus.PENDING,
                    PaymentStatus.FAILED,
                    PaymentStatus.EXPIRED,
                    PaymentStatus.CANCELLED
                }) {
            payment(booking, status, "NPR", "999.00", within);
        }
        payment(booking, PaymentStatus.SUCCESS, "NPR", "999.00", null);
        payment(booking, PaymentStatus.SUCCESS, "NPR", "999.00", END.plusDays(1).atStartOfDay());
        refund(refunded, booking, RefundStatus.SUCCEEDED, "NPR", "30.00", within.plusDays(1));
        refund(refunded, booking, RefundStatus.FAILED, "NPR", "40.00", within.plusDays(1));
        refund(refunded, booking, RefundStatus.SUCCEEDED, "INR", "25.00", within.plusDays(1));

        mockMvc.perform(report("/revenue", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currencies", hasSize(3)))
                .andExpect(
                        jsonPath(
                                "$.data.currencies[?(@.currency == 'NPR')].grossRevenue",
                                contains(150.00)))
                .andExpect(
                        jsonPath(
                                "$.data.currencies[?(@.currency == 'NPR')].refundAmount",
                                contains(30.00)))
                .andExpect(
                        jsonPath(
                                "$.data.currencies[?(@.currency == 'NPR')].netRevenue",
                                contains(120.00)))
                .andExpect(
                        jsonPath(
                                "$.data.currencies[?(@.currency == 'NPR')].successfulPaymentCount",
                                contains(2)))
                .andExpect(
                        jsonPath(
                                "$.data.currencies[?(@.currency == 'NPR')].successfulRefundCount",
                                contains(1)))
                .andExpect(
                        jsonPath(
                                "$.data.currencies[?(@.currency == 'NPR')].averageSuccessfulPaymentAmount",
                                contains(75.00)))
                .andExpect(
                        jsonPath(
                                "$.data.currencies[?(@.currency == 'INR')].netRevenue",
                                contains(-25.00)));

        mockMvc.perform(report("/revenue", admin).param("currency", " npr "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currencies", hasSize(1)))
                .andExpect(jsonPath("$.data.currencies[0].currency").value("NPR"));
    }

    @Test
    void combinedDashboardReturnsPeriodMetricsAndCurrentSnapshots() throws Exception {
        String admin = tokenFor("report-dashboard@example.com", Role.ADMIN);
        User customer = saveUser("period-customer@example.com", Role.CUSTOMER);
        customer.setCreatedAt(START.atStartOfDay());
        userRepository.save(customer);
        User staff = saveUser("period-staff@example.com", Role.STAFF);
        staff.setCreatedAt(START.atStartOfDay());
        userRepository.save(staff);
        saveMovie("Upcoming report movie", MovieStatus.UPCOMING);
        saveMovie("Ended report movie", MovieStatus.ENDED);
        show(ShowStatus.RUNNING);
        show(ShowStatus.SCHEDULED);

        mockMvc.perform(report("", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.period.startInclusive").value("2026-07-01T00:00:00"))
                .andExpect(jsonPath("$.data.period.endExclusive").value("2026-08-01T00:00:00"))
                .andExpect(jsonPath("$.data.period.timeZone").value(clock.getZone().getId()))
                .andExpect(jsonPath("$.data.registeredCustomers").value(1))
                .andExpect(jsonPath("$.data.activeMovies").value(3))
                .andExpect(jsonPath("$.data.runningShows").value(1))
                .andExpect(jsonPath("$.data.scheduledShows").value(1))
                .andExpect(jsonPath("$.data.generatedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.bookings").exists())
                .andExpect(jsonPath("$.data.revenue").exists());
    }

    @Test
    void openApiDocumentsReportingTagSecurityAndParameters() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/admin/reports/dashboard']").exists())
                .andExpect(jsonPath("$.paths['/api/admin/reports/dashboard/bookings']").exists())
                .andExpect(jsonPath("$.paths['/api/admin/reports/dashboard/revenue']").exists())
                .andExpect(
                        jsonPath(
                                "$.paths['/api/admin/reports/dashboard'].get.tags",
                                hasItem("Admin Reports")))
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/admin/reports/dashboard'].get.security[0].bearerAuth")
                                .exists())
                .andExpect(
                        jsonPath(
                                "$.paths['/api/admin/reports/dashboard'].get.parameters[*].name",
                                hasItems("startDate", "endDate", "currency")));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder report(
            String suffix, String token) {
        return get("/api/admin/reports/dashboard" + suffix)
                .header("Authorization", bearer(token))
                .param("startDate", START.toString())
                .param("endDate", END.toString());
    }

    private Show show(ShowStatus status) {
        var movie = saveMovie("Report Movie " + UUID.randomUUID(), MovieStatus.NOW_SHOWING);
        var hall = saveHall("Report Hall " + UUID.randomUUID(), Status.ACTIVE);
        Show show =
                Show.builder()
                        .movie(movie)
                        .hall(hall)
                        .status(status)
                        .showDate(END.plusDays(1))
                        .showTime(LocalTime.NOON)
                        .endTime(LocalTime.of(14, 0))
                        .build();
        show = showRepository.save(show);
        show.setStatus(status);
        return showRepository.save(show);
    }

    private Booking saveBooking(User user, Show show, BookingStatus status, LocalDateTime time) {
        Booking booking =
                Booking.builder()
                        .bookingReference("HCG-REPORT-" + UUID.randomUUID())
                        .user(user)
                        .show(show)
                        .bookingTime(time)
                        .status(status)
                        .totalAmount(new BigDecimal("100.00"))
                        .currency("NPR")
                        .build();
        return bookingRepository.save(booking);
    }

    private Payment payment(
            Booking booking,
            PaymentStatus status,
            String currency,
            String amount,
            LocalDateTime completedAt) {
        String reference = "PAY-REPORT-" + UUID.randomUUID();
        return paymentRepository.save(
                Payment.builder()
                        .booking(booking)
                        .paymentReference(reference)
                        .provider(PaymentProvider.LOCAL)
                        .method(PaymentMethod.ONLINE)
                        .status(status)
                        .amount(new BigDecimal(amount))
                        .currency(currency)
                        .providerTransactionId(reference)
                        .initiatedAt(START.atStartOfDay())
                        .completedAt(completedAt)
                        .build());
    }

    private Refund refund(
            Payment payment,
            Booking booking,
            RefundStatus status,
            String currency,
            String amount,
            LocalDateTime processedAt) {
        LocalDateTime requested = START.atStartOfDay();
        Refund refund =
                Refund.builder()
                        .payment(payment)
                        .booking(booking)
                        .refundReference("RFD-REPORT-" + UUID.randomUUID())
                        .amount(new BigDecimal(amount))
                        .currency(currency)
                        .status(status)
                        .reason(RefundReason.OTHER)
                        .type(RefundType.FULL)
                        .method(RefundMethod.MANUAL)
                        .idempotencyKey("report-" + UUID.randomUUID())
                        .requestedAt(requested)
                        .processedAt(processedAt)
                        .completedAt(processedAt)
                        .build();
        return refundRepository.save(refund);
    }
}
