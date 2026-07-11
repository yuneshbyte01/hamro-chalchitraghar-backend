package com.chalchitraghar.modules.bookings.mapper;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.chalchitraghar.modules.bookings.dto.response.AdminBookingDetailResponse;
import com.chalchitraghar.modules.bookings.dto.response.CustomerBookingDetailResponse;
import com.chalchitraghar.modules.bookings.dto.response.CustomerBookingSummaryResponse;
import com.chalchitraghar.modules.bookings.dto.response.StaffBookingDetailResponse;
import com.chalchitraghar.modules.bookings.entity.Booking;
import com.chalchitraghar.modules.seats.dto.response.SeatResponse;
import com.chalchitraghar.modules.seats.entity.Seat;
import com.chalchitraghar.modules.seats.mapper.SeatMapper;

import lombok.RequiredArgsConstructor;

/** Centralized audience-specific booking response mapping. */
@Component
@RequiredArgsConstructor
public class BookingMapper {

    private final SeatMapper seatMapper;

    public CustomerBookingSummaryResponse toCustomerSummary(Booking booking, List<Seat> seats) {
        List<Seat> orderedSeats = orderedSeats(seats);
        return new CustomerBookingSummaryResponse(
                booking.getId(), booking.getStatus(), booking.getShow().getId(),
                booking.getShow().getMovie().getTitle(), booking.getShow().getHall().getName(),
                showDateTime(booking), orderedSeats.stream().map(Seat::getSeatCode).toList(),
                totalPrice(orderedSeats), booking.getBookingTime());
    }

    public CustomerBookingDetailResponse toCustomerDetail(Booking booking, List<Seat> seats) {
        List<Seat> orderedSeats = orderedSeats(seats);
        return new CustomerBookingDetailResponse(
                booking.getId(), booking.getStatus(), booking.getShow().getId(),
                booking.getShow().getMovie().getTitle(), booking.getShow().getHall().getName(),
                showDateTime(booking), booking.getShow().getShowTime().toString(),
                booking.getShow().getEndTime().toString(), mapSeats(orderedSeats),
                totalPrice(orderedSeats), booking.getBookingTime());
    }

    public StaffBookingDetailResponse toStaffDetail(Booking booking, List<Seat> seats) {
        List<Seat> orderedSeats = orderedSeats(seats);
        return new StaffBookingDetailResponse(
                booking.getId(), booking.getStatus(), booking.getUser().getId(),
                booking.getUser().getName(), booking.getUser().getEmail(), booking.getShow().getId(),
                booking.getShow().getMovie().getTitle(), booking.getShow().getHall().getName(),
                showDateTime(booking), booking.getShow().getShowTime().toString(),
                booking.getShow().getEndTime().toString(), mapSeats(orderedSeats),
                totalPrice(orderedSeats), booking.getBookingTime(), booking.getCreatedAt(), booking.getUpdatedAt());
    }

    public AdminBookingDetailResponse toAdminDetail(Booking booking, List<Seat> seats) {
        List<Seat> orderedSeats = orderedSeats(seats);
        return new AdminBookingDetailResponse(
                booking.getId(), booking.getStatus(), booking.getUser().getId(),
                booking.getUser().getName(), booking.getUser().getEmail(), booking.getShow().getId(),
                booking.getShow().getMovie().getTitle(), booking.getShow().getHall().getName(),
                showDateTime(booking), booking.getShow().getShowTime().toString(),
                booking.getShow().getEndTime().toString(), mapSeats(orderedSeats),
                totalPrice(orderedSeats), booking.getBookingTime(), booking.getCreatedAt(), booking.getUpdatedAt());
    }

    private List<Seat> orderedSeats(List<Seat> seats) {
        return seats == null ? List.of() : seats.stream()
                .sorted(Comparator.comparing(Seat::getPositionIndex).thenComparing(Seat::getId))
                .toList();
    }

    private List<SeatResponse> mapSeats(List<Seat> seats) {
        return seats.stream().map(seatMapper::toResponseDto).toList();
    }

    private Double totalPrice(List<Seat> seats) {
        return seats.stream().mapToDouble(Seat::getPrice).sum();
    }

    private LocalDateTime showDateTime(Booking booking) {
        return LocalDateTime.of(booking.getShow().getShowDate(), booking.getShow().getShowTime());
    }
}
