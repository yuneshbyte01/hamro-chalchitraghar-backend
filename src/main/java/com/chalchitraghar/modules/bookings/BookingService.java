package com.chalchitraghar.modules.bookings;

import java.util.List;

import com.chalchitraghar.modules.bookings.BookingRequest;
import com.chalchitraghar.modules.bookings.BookingResponse;
import com.chalchitraghar.modules.users.User;

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
