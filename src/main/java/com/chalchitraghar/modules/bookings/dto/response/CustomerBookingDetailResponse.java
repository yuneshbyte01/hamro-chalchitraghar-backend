package com.chalchitraghar.modules.bookings.dto.response;

import com.chalchitraghar.modules.bookings.enums.BookingStatus;
import com.chalchitraghar.modules.seats.dto.response.SeatResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer-safe booking detail; contains no customer identity or audit fields")
public class CustomerBookingDetailResponse {
    private Long bookingId;
    private String bookingReference;
    private BookingStatus bookingStatus;
    private Long showId;
    private String movieName;
    private String hallName;
    private LocalDateTime showDateTime;
    private String startTime;
    private String endTime;
    private List<SeatResponse> selectedSeats;
    private BigDecimal totalAmount;
    private String currency;
    private LocalDateTime bookingTime;
    private LocalDateTime expiresAt;
}
