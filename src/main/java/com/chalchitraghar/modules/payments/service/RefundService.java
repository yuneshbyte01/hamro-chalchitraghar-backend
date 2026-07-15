package com.chalchitraghar.modules.payments.service;

import com.chalchitraghar.modules.payments.dto.request.*;
import com.chalchitraghar.modules.payments.dto.response.*;
import com.chalchitraghar.modules.payments.entity.Refund;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.shared.response.PageResponse;
import java.util.List;

public interface RefundService {
    Refund createRefundIntent(CreateRefundIntentCommand command);

    AdminRefundDetailResponse createAdminRefund(
            AdminCreateRefundRequest request, String idempotencyKey, User admin);

    AdminRefundDetailResponse approveRefund(String reference, User admin);

    AdminRefundDetailResponse rejectRefund(
            String reference, AdminRejectRefundRequest request, User admin);

    PageResponse<CustomerRefundSummaryResponse> getCustomerRefunds(
            CustomerRefundFilter filter, int page, int size, User user);

    CustomerRefundDetailResponse getCustomerRefund(String reference, User user);

    List<CustomerRefundSummaryResponse> getCustomerBookingRefunds(
            String bookingReference, User user);

    PageResponse<AdminRefundSummaryResponse> getAdminRefunds(
            AdminRefundFilter filter, int page, int size);

    AdminRefundDetailResponse getAdminRefund(String reference);
}
