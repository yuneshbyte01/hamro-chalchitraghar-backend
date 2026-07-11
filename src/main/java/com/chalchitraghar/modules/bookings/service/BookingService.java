package com.chalchitraghar.modules.bookings.service;

import java.util.List;

import com.chalchitraghar.modules.bookings.dto.request.BookingRequest;
import com.chalchitraghar.modules.bookings.dto.response.AdminBookingDetailResponse;
import com.chalchitraghar.modules.bookings.dto.response.CustomerBookingDetailResponse;
import com.chalchitraghar.modules.bookings.dto.response.CustomerBookingSummaryResponse;
import com.chalchitraghar.modules.bookings.dto.response.StaffBookingDetailResponse;
import com.chalchitraghar.modules.users.entity.User;

/**
 * Service for managing booking operations.
 */
public interface BookingService {

    CustomerBookingDetailResponse createBooking(BookingRequest request, User user);

    CustomerBookingDetailResponse confirmBooking(Long bookingId, User user);

    List<CustomerBookingSummaryResponse> getCustomerBookings(User user);

    CustomerBookingDetailResponse cancelBooking(Long bookingId, User user);

    CustomerBookingDetailResponse getCustomerBookingById(Long bookingId, User user);

    StaffBookingDetailResponse getStaffBookingById(Long bookingId);

    AdminBookingDetailResponse getAdminBookingById(Long bookingId);
}
