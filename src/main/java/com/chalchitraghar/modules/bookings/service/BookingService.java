package com.chalchitraghar.modules.bookings.service;

import java.util.List;

import com.chalchitraghar.modules.bookings.dto.request.BookingRequest;
import com.chalchitraghar.modules.bookings.dto.response.BookingResponse;
import com.chalchitraghar.modules.users.entity.User;

/**
 * Service for managing booking operations.
 */
public interface BookingService {

    BookingResponse createBooking(BookingRequest request, User user);

    BookingResponse confirmBooking(Long bookingId, User user);

    List<BookingResponse> getMyBookings(User user);

    BookingResponse cancelBooking(Long bookingId, User user);

    BookingResponse getBookingById(Long bookingId, User user);
}
