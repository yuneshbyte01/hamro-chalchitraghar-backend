package com.chalchitraghar.modules.payments.service;
import java.util.List;
import com.chalchitraghar.modules.payments.dto.response.*;
import com.chalchitraghar.modules.users.entity.User;
public interface PaymentService {
    List<CustomerPaymentSummaryResponse> getMyPayments(User user);
    CustomerPaymentDetailResponse getMyPaymentByReference(String reference, User user);
    List<CustomerPaymentSummaryResponse> getMyPaymentsByBookingReference(String bookingReference, User user);
    StaffPaymentDetailResponse getStaffPaymentByReference(String reference);
    List<StaffPaymentSummaryResponse> getStaffPaymentsByBookingReference(String bookingReference);
    AdminPaymentDetailResponse getAdminPaymentByReference(String reference);
    List<AdminPaymentSummaryResponse> getAdminPaymentsByBookingReference(String bookingReference);
}
