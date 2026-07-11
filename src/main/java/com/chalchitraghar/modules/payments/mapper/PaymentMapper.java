package com.chalchitraghar.modules.payments.mapper;

import org.springframework.stereotype.Component;
import com.chalchitraghar.modules.payments.dto.response.*;
import com.chalchitraghar.modules.payments.entity.Payment;

@Component
public class PaymentMapper {
    public CustomerPaymentSummaryResponse toCustomerSummary(Payment p) { return new CustomerPaymentSummaryResponse(
            p.getPaymentReference(), p.getBooking().getBookingReference(), p.getProvider(), p.getMethod(), p.getStatus(),
            p.getAmount(), p.getCurrency(), p.getInitiatedAt(), p.getCompletedAt()); }
    public CustomerPaymentDetailResponse toCustomerDetail(Payment p) { return new CustomerPaymentDetailResponse(
            p.getPaymentReference(), p.getBooking().getBookingReference(), p.getProvider(), p.getMethod(), p.getStatus(),
            p.getAmount(), p.getCurrency(), p.getProviderTransactionId(), p.getInitiatedAt(), p.getExpiresAt(), p.getCompletedAt(),
            p.getFailedAt(), p.getExpiredAt(), p.getCancelledAt(), p.getFailureMessage(), p.getProviderStatus(),
            p.getVerificationTime(), p.isManualReviewRequired()); }
    public StaffPaymentSummaryResponse toStaffSummary(Payment p) { var u=p.getBooking().getUser(); return new StaffPaymentSummaryResponse(
            p.getPaymentReference(), p.getBooking().getBookingReference(), u.getId(), u.getName(), u.getEmail(), p.getProvider(),
            p.getMethod(), p.getStatus(), p.getAmount(), p.getCurrency(), p.getInitiatedAt(), p.getCompletedAt()); }
    public StaffPaymentDetailResponse toStaffDetail(Payment p) { var u=p.getBooking().getUser(); return new StaffPaymentDetailResponse(
            p.getPaymentReference(), p.getBooking().getBookingReference(), u.getId(), u.getName(), u.getEmail(), p.getProvider(),
            p.getMethod(), p.getStatus(), p.getAmount(), p.getCurrency(), p.getInitiatedAt(), p.getCompletedAt(),
            p.getProviderTransactionId(), p.getFailureCode(), p.getFailureMessage(), p.getFailedAt(), p.getExpiredAt(),
            p.getCancelledAt(), p.getCreatedAt(), p.getUpdatedAt(), p.getProviderReference(), p.getProviderStatus(),
            p.getVerificationTime(), p.isManualReviewRequired(), p.getManualReviewReason()); }
    public AdminPaymentSummaryResponse toAdminSummary(Payment p) { var u=p.getBooking().getUser(); return new AdminPaymentSummaryResponse(
            p.getPaymentReference(), p.getBooking().getBookingReference(), u.getId(), u.getName(), u.getEmail(), p.getProvider(),
            p.getMethod(), p.getStatus(), p.getAmount(), p.getCurrency(), p.getInitiatedAt(), p.getCompletedAt()); }
    public AdminPaymentDetailResponse toAdminDetail(Payment p) { var u=p.getBooking().getUser(); return new AdminPaymentDetailResponse(
            p.getPaymentReference(), p.getBooking().getBookingReference(), u.getId(), u.getName(), u.getEmail(), p.getProvider(),
            p.getMethod(), p.getStatus(), p.getAmount(), p.getCurrency(), p.getInitiatedAt(), p.getCompletedAt(),
            p.getProviderTransactionId(), p.getFailureCode(), p.getFailureMessage(), p.getFailedAt(), p.getExpiredAt(),
            p.getCancelledAt(), p.getCreatedAt(), p.getUpdatedAt(), p.getProviderReference(), p.getProviderStatus(),
            p.getVerificationTime(), p.isManualReviewRequired(), p.getManualReviewReason()); }
}
