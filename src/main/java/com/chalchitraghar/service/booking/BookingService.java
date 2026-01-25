package com.chalchitraghar.service.booking;

import java.util.List;

import com.chalchitraghar.dto.booking.BookingRequest;
import com.chalchitraghar.dto.booking.BookingResponse;
import com.chalchitraghar.model.User;

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
