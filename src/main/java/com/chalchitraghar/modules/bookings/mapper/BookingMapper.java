package com.chalchitraghar.modules.bookings.mapper;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.chalchitraghar.modules.bookings.dto.response.AdminBookingDetailResponse;
import com.chalchitraghar.modules.bookings.dto.response.AdminBookingSummaryResponse;
import com.chalchitraghar.modules.bookings.dto.response.CustomerBookingDetailResponse;
import com.chalchitraghar.modules.bookings.dto.response.CustomerBookingSummaryResponse;
import com.chalchitraghar.modules.bookings.dto.response.StaffBookingDetailResponse;
import com.chalchitraghar.modules.bookings.dto.response.StaffBookingSummaryResponse;
import com.chalchitraghar.modules.bookings.entity.Booking;
import com.chalchitraghar.modules.bookings.entity.BookingSeat;
import com.chalchitraghar.modules.seats.dto.response.SeatResponse;
import com.chalchitraghar.modules.seats.mapper.SeatMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BookingMapper {
    private final SeatMapper seatMapper;

    public CustomerBookingSummaryResponse toCustomerSummary(Booking b, List<BookingSeat> seats) {
        var ordered = ordered(seats);
        return new CustomerBookingSummaryResponse(b.getId(), b.getBookingReference(), b.getStatus(), b.getShow().getId(),
                b.getShow().getMovie().getTitle(), b.getShow().getHall().getName(), showDateTime(b),
                ordered.stream().map(bs -> bs.getSeat().getSeatCode()).toList(), b.getTotalAmount(), b.getCurrency(),
                b.getBookingTime(), b.getExpiresAt());
    }

    public CustomerBookingDetailResponse toCustomerDetail(Booking b, List<BookingSeat> seats) {
        return new CustomerBookingDetailResponse(b.getId(), b.getBookingReference(), b.getStatus(), b.getShow().getId(),
                b.getShow().getMovie().getTitle(), b.getShow().getHall().getName(), showDateTime(b),
                b.getShow().getShowTime().toString(), b.getShow().getEndTime().toString(), mapSeats(seats),
                b.getTotalAmount(), b.getCurrency(), b.getBookingTime(), b.getExpiresAt());
    }

    public StaffBookingSummaryResponse toStaffSummary(Booking b, List<BookingSeat> seats) {
        var ordered = ordered(seats);
        return new StaffBookingSummaryResponse(b.getId(), b.getBookingReference(), b.getStatus(), b.getUser().getId(),
                b.getUser().getName(), b.getUser().getEmail(), b.getShow().getId(), b.getShow().getMovie().getTitle(),
                b.getShow().getHall().getName(), showDateTime(b),
                ordered.stream().map(bs -> bs.getSeat().getSeatCode()).toList(), b.getTotalAmount(), b.getCurrency(),
                b.getBookingTime(), b.getExpiresAt());
    }

    public AdminBookingSummaryResponse toAdminSummary(Booking b, List<BookingSeat> seats) {
        var ordered = ordered(seats);
        return new AdminBookingSummaryResponse(b.getId(), b.getBookingReference(), b.getStatus(), b.getUser().getId(),
                b.getUser().getName(), b.getUser().getEmail(), b.getShow().getId(), b.getShow().getMovie().getTitle(),
                b.getShow().getHall().getName(), showDateTime(b),
                ordered.stream().map(bs -> bs.getSeat().getSeatCode()).toList(), b.getTotalAmount(), b.getCurrency(),
                b.getBookingTime(), b.getExpiresAt());
    }

    public StaffBookingDetailResponse toStaffDetail(Booking b, List<BookingSeat> seats) {
        return new StaffBookingDetailResponse(b.getId(), b.getBookingReference(), b.getStatus(), b.getUser().getId(),
                b.getUser().getName(), b.getUser().getEmail(), b.getShow().getId(), b.getShow().getMovie().getTitle(),
                b.getShow().getHall().getName(), showDateTime(b), b.getShow().getShowTime().toString(),
                b.getShow().getEndTime().toString(), mapSeats(seats), b.getTotalAmount(), b.getCurrency(),
                b.getBookingTime(), b.getExpiresAt(), b.getConfirmedAt(), b.getCancelledAt(), b.getExpiredAt(),
                b.getCreatedAt(), b.getUpdatedAt());
    }

    public AdminBookingDetailResponse toAdminDetail(Booking b, List<BookingSeat> seats) {
        return new AdminBookingDetailResponse(b.getId(), b.getBookingReference(), b.getStatus(), b.getUser().getId(),
                b.getUser().getName(), b.getUser().getEmail(), b.getShow().getId(), b.getShow().getMovie().getTitle(),
                b.getShow().getHall().getName(), showDateTime(b), b.getShow().getShowTime().toString(),
                b.getShow().getEndTime().toString(), mapSeats(seats), b.getTotalAmount(), b.getCurrency(),
                b.getBookingTime(), b.getExpiresAt(), b.getConfirmedAt(), b.getCancelledAt(), b.getExpiredAt(),
                b.getCreatedAt(), b.getUpdatedAt());
    }

    private List<BookingSeat> ordered(List<BookingSeat> seats) {
        return seats == null ? List.of() : seats.stream()
                .sorted(Comparator.comparing(bs -> bs.getSeat().getPositionIndex())).toList();
    }

    private List<SeatResponse> mapSeats(List<BookingSeat> seats) {
        return ordered(seats).stream().map(bs -> {
            SeatResponse response = seatMapper.toResponseDto(bs.getSeat());
            response.setPrice(bs.getUnitPrice());
            return response;
        }).toList();
    }

    private LocalDateTime showDateTime(Booking booking) {
        return LocalDateTime.of(booking.getShow().getShowDate(), booking.getShow().getShowTime());
    }
}
