package com.chalchitraghar.modules.bookings.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.chalchitraghar.modules.bookings.enums.BookingStatus;
import com.chalchitraghar.modules.seats.dto.response.SeatResponse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer-safe booking detail; contains no customer identity or audit fields")
public class CustomerBookingDetailResponse {
    private Long bookingId;
    private BookingStatus bookingStatus;
    private Long showId;
    private String movieName;
    private String hallName;
    private LocalDateTime showDateTime;
    private String startTime;
    private String endTime;
    private List<SeatResponse> selectedSeats;
    private Double totalPrice;
    private LocalDateTime bookingTime;
}
