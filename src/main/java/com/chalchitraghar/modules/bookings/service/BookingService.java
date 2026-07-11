package com.chalchitraghar.modules.bookings.service;

import com.chalchitraghar.modules.bookings.dto.request.BookingRequest;
import com.chalchitraghar.modules.bookings.dto.request.BookingSearchCriteria;
import com.chalchitraghar.modules.bookings.dto.response.AdminBookingDetailResponse;
import com.chalchitraghar.modules.bookings.dto.response.AdminBookingSummaryResponse;
import com.chalchitraghar.modules.bookings.dto.response.CustomerBookingDetailResponse;
import com.chalchitraghar.modules.bookings.dto.response.CustomerBookingSummaryResponse;
import com.chalchitraghar.modules.bookings.dto.response.StaffBookingDetailResponse;
import com.chalchitraghar.modules.bookings.dto.response.StaffBookingSummaryResponse;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.shared.response.PageResponse;

/**
 * Service for managing booking operations.
 */
public interface BookingService {

    CustomerBookingDetailResponse createBooking(BookingRequest request, User user);

    CustomerBookingDetailResponse confirmBooking(Long bookingId, User user);

    PageResponse<CustomerBookingSummaryResponse> getCustomerBookings(
            User user, BookingSearchCriteria criteria, int page, int size, String sortBy, String sortDir);

    CustomerBookingDetailResponse cancelBooking(Long bookingId, User user);

    CustomerBookingDetailResponse getCustomerBookingById(Long bookingId, User user);

    StaffBookingDetailResponse getStaffBookingById(Long bookingId);

    PageResponse<StaffBookingSummaryResponse> getStaffBookings(
            BookingSearchCriteria criteria, int page, int size, String sortBy, String sortDir);

    AdminBookingDetailResponse getAdminBookingById(Long bookingId);

    PageResponse<AdminBookingSummaryResponse> getAdminBookings(
            BookingSearchCriteria criteria, int page, int size, String sortBy, String sortDir);
}
