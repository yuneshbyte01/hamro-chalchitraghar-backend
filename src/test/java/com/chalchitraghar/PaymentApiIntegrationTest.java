package com.chalchitraghar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import com.chalchitraghar.modules.bookings.entity.Booking;
import com.chalchitraghar.modules.bookings.enums.BookingStatus;
import com.chalchitraghar.modules.movies.enums.MovieStatus;
import com.chalchitraghar.modules.halls.enums.Status;
import com.chalchitraghar.modules.payments.entity.Payment;
import com.chalchitraghar.modules.payments.enums.*;
import com.chalchitraghar.modules.shows.entity.Show;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.modules.users.enums.Role;

class PaymentApiIntegrationTest extends AbstractIntegrationTest {

    @Test void initiatesEsewaSignedFormWithServerValues() throws Exception {
        Context c=context("esewa-init@example.com");
        mockMvc.perform(post("/api/customer/bookings/{ref}/payments",c.booking.getBookingReference())
                .header("Authorization",bearer(c.token)).header("Idempotency-Key","esewa-init")
                .contentType("application/json").content("{\"provider\":\"ESEWA\",\"method\":\"ONLINE\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.amount").value("500"))
                .andExpect(jsonPath("$.data.taxAmount").value("0"))
                .andExpect(jsonPath("$.data.productServiceCharge").value("0"))
                .andExpect(jsonPath("$.data.productDeliveryCharge").value("0"))
                .andExpect(jsonPath("$.data.productCode").value("EPAYTEST"))
                .andExpect(jsonPath("$.data.signedFieldNames").value("total_amount,transaction_uuid,product_code"))
                .andExpect(jsonPath("$.data.transactionUuid").value(org.hamcrest.Matchers.startsWith("PAY-")))
                .andExpect(jsonPath("$.data.signature").isNotEmpty());
    }

    @Test void adminOperationalEndpointsExposeReviewStatisticsAndResolution() throws Exception {
        Context c=context("ops-payment@example.com"); Payment p=payment(c.booking,"PAY-20260711-OPS00001",new BigDecimal("500"));
        p.setStatus(PaymentStatus.SUCCESS);p.setManualReviewRequired(true);p.setManualReviewReason("test review");paymentRepository.saveAndFlush(p);
        String admin=tokenFor("ops-admin@example.com",Role.ADMIN);
        mockMvc.perform(get("/api/admin/payments").header("Authorization",bearer(admin)).param("status","SUCCESS"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));
        mockMvc.perform(get("/api/admin/payments/manual-review").header("Authorization",bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));
        mockMvc.perform(get("/api/admin/payments/statistics").header("Authorization",bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.successful").value(1));
        mockMvc.perform(post("/api/admin/payments/{ref}/resolve",p.getPaymentReference()).header("Authorization",bearer(admin)).param("resolution","CLEAR"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.manualReviewRequired").value(false));
        mockMvc.perform(get("/api/admin/payments/statistics").header("Authorization",bearer(c.token))).andExpect(status().isForbidden());
    }

    @Test void initiatesFromServerAmountAndIsIdempotent() throws Exception {
        Context c=context("init-payment@example.com");
        String body="{\"provider\":\"LOCAL\",\"method\":\"ONLINE\",\"amount\":1,\"currency\":\"USD\"}";
        String first=mockMvc.perform(post("/api/customer/bookings/{ref}/payments",c.booking.getBookingReference())
                .header("Authorization",bearer(c.token)).header("Idempotency-Key"," init-key ")
                .contentType("application/json").content(body)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.amount").value(500.00)).andExpect(jsonPath("$.data.currency").value("NPR"))
                .andExpect(jsonPath("$.data.status").value("PENDING")).andExpect(jsonPath("$.data.expiresAt").exists())
                .andReturn().getResponse().getContentAsString();
        String ref=objectMapper.readTree(first).path("data").path("paymentReference").asText();
        mockMvc.perform(post("/api/customer/bookings/{ref}/payments",c.booking.getBookingReference())
                .header("Authorization",bearer(c.token)).header("Idempotency-Key","init-key")
                .contentType("application/json").content(body)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.paymentReference").value(ref));
        assertThat(paymentRepository.count()).isEqualTo(1);
    }

    @Test void initiationValidatesOwnershipStateKeysAndActiveAttempt() throws Exception {
        Context owner=context("init-owner@example.com"), other=context("init-intruder@example.com"); String body="{\"provider\":\"LOCAL\",\"method\":\"ONLINE\"}";
        mockMvc.perform(post("/api/customer/bookings/{ref}/payments",owner.booking.getBookingReference()).contentType("application/json").content(body)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/customer/bookings/{ref}/payments",owner.booking.getBookingReference()).header("Authorization",bearer(other.token)).header("Idempotency-Key","x").contentType("application/json").content(body)).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/customer/bookings/{ref}/payments",owner.booking.getBookingReference()).header("Authorization",bearer(owner.token)).header("Idempotency-Key","   ").contentType("application/json").content(body)).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/customer/bookings/{ref}/payments",owner.booking.getBookingReference()).header("Authorization",bearer(owner.token)).header("Idempotency-Key","a".repeat(256)).contentType("application/json").content(body)).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/customer/bookings/{ref}/payments",owner.booking.getBookingReference()).header("Authorization",bearer(owner.token)).header("Idempotency-Key","one").contentType("application/json").content(body)).andExpect(status().isCreated());
        mockMvc.perform(post("/api/customer/bookings/{ref}/payments",owner.booking.getBookingReference()).header("Authorization",bearer(owner.token)).header("Idempotency-Key","two").contentType("application/json").content(body)).andExpect(status().isConflict());
        mockMvc.perform(post("/api/customer/bookings/{ref}/payments",owner.booking.getBookingReference()).header("Authorization",bearer(owner.token)).header("Idempotency-Key","one").contentType("application/json").content("{\"provider\":\"LOCAL\",\"method\":\"CASH\"}")).andExpect(status().isConflict());
    }

    @Test void localProcessSupportsSuccessFailureAndExpiryWithoutConfirmingBooking() throws Exception {
        Context success=context("local-success@example.com"); Payment p=payment(success.booking,"PAY-20260711-LOCAL001",new BigDecimal("500")); p.setStatus(PaymentStatus.PENDING); p.setExpiresAt(LocalDateTime.now(clock).plusMinutes(5)); paymentRepository.saveAndFlush(p);
        mockMvc.perform(post("/api/customer/payments/{ref}/process",p.getPaymentReference()).header("Authorization",bearer(success.token)).contentType("application/json").content("{\"result\":\"SUCCESS\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("SUCCESS")).andExpect(jsonPath("$.data.completedAt").exists());
        assertThat(bookingRepository.findById(success.booking.getId()).orElseThrow().getStatus()).isEqualTo(BookingStatus.INITIATED);
        Context failed=context("local-failed@example.com"); Payment f=payment(failed.booking,"PAY-20260711-LOCAL002",new BigDecimal("500")); f.setStatus(PaymentStatus.PENDING); f.setExpiresAt(LocalDateTime.now(clock).plusMinutes(5)); paymentRepository.saveAndFlush(f);
        mockMvc.perform(post("/api/customer/payments/{ref}/process",f.getPaymentReference()).header("Authorization",bearer(failed.token)).contentType("application/json").content("{\"result\":\"FAILED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("FAILED")).andExpect(jsonPath("$.data.failureMessage").exists());
        Context expired=context("local-expired@example.com"); Payment e=payment(expired.booking,"PAY-20260711-LOCAL003",new BigDecimal("500")); e.setStatus(PaymentStatus.PENDING); e.setExpiresAt(LocalDateTime.now(clock).minusSeconds(1)); paymentRepository.saveAndFlush(e);
        mockMvc.perform(post("/api/customer/payments/{ref}/process",e.getPaymentReference()).header("Authorization",bearer(expired.token)).contentType("application/json").content("{\"result\":\"SUCCESS\"}")).andExpect(status().isConflict());
        assertThat(paymentRepository.findById(e.getId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.EXPIRED);
    }

    @Test void cancellationIsOwnerOnlyIdempotentAndAllowsRetry() throws Exception {
        Context c=context("cancel-payment@example.com"); Payment p=payment(c.booking,"PAY-20260711-CANCEL01",new BigDecimal("500")); p.setStatus(PaymentStatus.PENDING); p.setExpiresAt(LocalDateTime.now(clock).plusMinutes(5)); paymentRepository.saveAndFlush(p);
        mockMvc.perform(post("/api/customer/payments/{ref}/cancel",p.getPaymentReference()).header("Authorization",bearer(c.token))).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("CANCELLED"));
        mockMvc.perform(post("/api/customer/payments/{ref}/cancel",p.getPaymentReference()).header("Authorization",bearer(c.token))).andExpect(status().isOk());
        mockMvc.perform(post("/api/customer/bookings/{ref}/payments",c.booking.getBookingReference()).header("Authorization",bearer(c.token)).header("Idempotency-Key","retry-after-cancel").contentType("application/json").content("{\"provider\":\"LOCAL\",\"method\":\"ONLINE\"}")).andExpect(status().isCreated());
    }

    @Test void persistsExactPaymentDataAndNullableTransactionId() throws Exception {
        Context c=context("pay-persist@example.com"); Payment p=payment(c.booking,"PAY-20260711-A1B2C3D4",new BigDecimal("425.50"));
        Payment saved=paymentRepository.saveAndFlush(p);
        assertThat(saved.getAmount()).isEqualByComparingTo("425.50"); assertThat(saved.getCurrency()).isEqualTo("NPR");
        assertThat(saved.getProvider()).isEqualTo(PaymentProvider.LOCAL); assertThat(saved.getMethod()).isEqualTo(PaymentMethod.ONLINE);
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.CREATED); assertThat(saved.getProviderTransactionId()).isNull();
    }

    @Test void rejectsDuplicateReferenceAndInvalidAmount() throws Exception {
        Context c=context("pay-constraints@example.com"); paymentRepository.saveAndFlush(payment(c.booking,"PAY-20260711-DUPL0001",new BigDecimal("10.00")));
        assertThrows(DataIntegrityViolationException.class,()->paymentRepository.saveAndFlush(payment(c.booking,"PAY-20260711-DUPL0001",new BigDecimal("11.00"))));
        paymentRepository.deleteAll();
        assertThrows(RuntimeException.class,()->paymentRepository.saveAndFlush(payment(c.booking,"PAY-20260711-ZERO0001",BigDecimal.ZERO)));
    }

    @Test void customerListsOnlyOwnPaymentsNewestFirstAndPayloadIsSafe() throws Exception {
        Context mine=context("pay-owner@example.com"); Context other=context("pay-other@example.com");
        Payment older=payment(mine.booking,"PAY-20260711-OLDER001",new BigDecimal("100.00")); paymentRepository.saveAndFlush(older);
        Payment newer=payment(mine.booking,"PAY-20260711-NEWER001",new BigDecimal("200.00")); paymentRepository.saveAndFlush(newer);
        paymentRepository.saveAndFlush(payment(other.booking,"PAY-20260711-OTHER001",new BigDecimal("300.00")));
        mockMvc.perform(get("/api/customer/payments").header("Authorization",bearer(mine.token)))
          .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2))
          .andExpect(jsonPath("$.data[0].paymentReference").value("PAY-20260711-NEWER001"))
          .andExpect(jsonPath("$.data[1].paymentReference").value("PAY-20260711-OLDER001"))
          .andExpect(jsonPath("$.data[0].customerEmail").doesNotExist()).andExpect(jsonPath("$.data[0].failureCode").doesNotExist())
          .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("password"))))
          .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("googleId"))));
    }

    @Test void customerReadsOwnReferenceAndBookingHistoryButOtherOwnerGets404() throws Exception {
        Context owner=context("pay-detail-owner@example.com"); Context other=context("pay-detail-other@example.com");
        paymentRepository.saveAndFlush(payment(owner.booking,"PAY-20260711-DETAIL01",new BigDecimal("250.00")));
        mockMvc.perform(get("/api/customer/payments/PAY-20260711-DETAIL01").header("Authorization",bearer(owner.token))).andExpect(status().isOk()).andExpect(jsonPath("$.data.bookingReference").value(owner.booking.getBookingReference()));
        mockMvc.perform(get("/api/customer/bookings/{ref}/payments",owner.booking.getBookingReference()).header("Authorization",bearer(owner.token))).andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1));
        mockMvc.perform(get("/api/customer/payments/PAY-20260711-DETAIL01").header("Authorization",bearer(other.token))).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/customer/bookings/{ref}/payments",owner.booking.getBookingReference()).header("Authorization",bearer(other.token))).andExpect(status().isNotFound());
    }

    @Test void paymentRoutesEnforceAuthenticationAndRoles() throws Exception {
        Context owner=context("pay-security@example.com"); paymentRepository.saveAndFlush(payment(owner.booking,"PAY-20260711-SECURE01",new BigDecimal("50.00")));
        String customer=owner.token, staff=tokenFor("payment-staff@example.com",Role.STAFF), admin=tokenFor("payment-admin@example.com",Role.ADMIN);
        mockMvc.perform(get("/api/customer/payments")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/customer/payments").header("Authorization","Bearer invalid.jwt.token")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/staff/payments/PAY-20260711-SECURE01").header("Authorization",bearer(customer))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/staff/payments/PAY-20260711-SECURE01").header("Authorization",bearer(staff))).andExpect(status().isOk()).andExpect(jsonPath("$.data.customerEmail").value(owner.user.getEmail()));
        mockMvc.perform(get("/api/admin/payments/PAY-20260711-SECURE01").header("Authorization",bearer(staff))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/payments/PAY-20260711-SECURE01").header("Authorization",bearer(customer))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/payments/PAY-20260711-SECURE01")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/payments/PAY-20260711-SECURE01").header("Authorization",bearer(admin))).andExpect(status().isOk());
    }

    @Test void staffAndAdminReadNewestBookingHistory() throws Exception {
        Context c=context("pay-history@example.com"); paymentRepository.saveAndFlush(payment(c.booking,"PAY-20260711-HIST0001",new BigDecimal("50.00"))); paymentRepository.saveAndFlush(payment(c.booking,"PAY-20260711-HIST0002",new BigDecimal("60.00")));
        String staff=tokenFor("history-staff@example.com",Role.STAFF), admin=tokenFor("history-admin@example.com",Role.ADMIN);
        mockMvc.perform(get("/api/staff/bookings/{ref}/payments",c.booking.getBookingReference()).header("Authorization",bearer(staff))).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].paymentReference").value("PAY-20260711-HIST0002")).andExpect(jsonPath("$.data[0].customerId").value(c.user.getId()));
        mockMvc.perform(get("/api/admin/bookings/{ref}/payments",c.booking.getBookingReference()).header("Authorization",bearer(admin))).andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2));
    }

    private Context context(String email) throws Exception {
        User user=saveUser(email,Role.CUSTOMER); String token=loginToken(email); String admin=tokenFor("admin-"+email,Role.ADMIN);
        Show show=saveShowWithSeats(saveMovie("Movie "+email,MovieStatus.NOW_SHOWING),saveHall("Hall "+email,Status.ACTIVE),admin);
        Booking booking=bookingRepository.save(Booking.builder().user(user).show(show).bookingReference("HCG-20260711-"+Math.abs(email.hashCode()))
                .bookingTime(LocalDateTime.now(clock)).status(BookingStatus.INITIATED).totalAmount(new BigDecimal("500.00"))
                .currency("NPR").expiresAt(LocalDateTime.now(clock).plusMinutes(15)).build());
        return new Context(user,token,booking);
    }
    private Payment payment(Booking b,String ref,BigDecimal amount){return Payment.builder().booking(b).paymentReference(ref).provider(PaymentProvider.LOCAL).method(PaymentMethod.ONLINE).status(PaymentStatus.CREATED).amount(amount).currency("NPR").initiatedAt(LocalDateTime.now(clock)).build();}
    private record Context(User user,String token,Booking booking){}
}
