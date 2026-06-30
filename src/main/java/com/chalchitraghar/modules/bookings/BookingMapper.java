package com.chalchitraghar.modules.bookings;

import com.chalchitraghar.modules.seats.SeatMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.chalchitraghar.modules.bookings.BookingResponse;
import com.chalchitraghar.modules.seats.SeatResponse;
import com.chalchitraghar.modules.bookings.Booking;
import com.chalchitraghar.modules.seats.Seat;

import lombok.RequiredArgsConstructor;

/**
 * Mapper for converting Booking entity to DTOs.
 */
@Component
@RequiredArgsConstructor
public class BookingMapper {

    private final SeatMapper seatMapper;

    /**
     * Converts a Booking entity with associated seats to a BookingResponse DTO.
     *
     * @param booking the booking entity to convert
     * @param seats the list of seats associated with the booking
     * @return the response DTO, or null if booking is null
     */
    public BookingResponse toResponseDto(Booking booking, List<Seat> seats) {
        if (booking == null) {
            return null;
        }

        // Map seats to response DTOs
        List<SeatResponse> seatResponses = seats != null
                ? seats.stream()
                        .map(seatMapper::toResponseDto)
                        .collect(Collectors.toList())
                : List.of();

        // Calculate total price from individual seat prices
        Double totalPrice = seats != null 
                ? seats.stream()
                        .mapToDouble(Seat::getPrice)
                        .sum()
                : 0.0;

        // Build response
        BookingResponse response = new BookingResponse();
        response.setBookingId(booking.getId());
        response.setBookingStatus(booking.getStatus());
        response.setShowId(booking.getShow().getId());
        response.setMovieName(booking.getShow().getMovie().getTitle());
        response.setHallName(booking.getShow().getHall().getName());
        response.setShowDateTime(LocalDateTime.of(
                booking.getShow().getShowDate(),
                booking.getShow().getShowTime()));
        response.setStartTime(booking.getShow().getShowTime().toString());
        response.setEndTime(booking.getShow().getEndTime().toString());
        response.setSelectedSeats(seatResponses);
        response.setTotalPrice(totalPrice);
        response.setBookingTime(booking.getBookingTime());

        return response;
    }
}
