package com.chalchitraghar.modules.bookings.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.chalchitraghar.modules.bookings.enums.BookingStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Admin operational booking summary")
public class AdminBookingSummaryResponse {
    private Long bookingId;
    private BookingStatus bookingStatus;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private Long showId;
    private String movieName;
    private String hallName;
    private LocalDateTime showDateTime;
    private List<String> selectedSeatCodes;
    private Double totalPrice;
    private LocalDateTime bookingTime;
}
