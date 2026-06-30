package com.chalchitraghar.modules.bookings.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.chalchitraghar.modules.seats.dto.response.SeatResponse;
import com.chalchitraghar.modules.bookings.enums.BookingStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO containing booking information with show details and selected seats.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

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
