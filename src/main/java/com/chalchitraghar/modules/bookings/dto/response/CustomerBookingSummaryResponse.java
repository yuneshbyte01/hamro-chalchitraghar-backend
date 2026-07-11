package com.chalchitraghar.modules.bookings.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

import com.chalchitraghar.modules.bookings.enums.BookingStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer-safe booking summary")
public class CustomerBookingSummaryResponse {
    private Long bookingId;
    private String bookingReference;
    private BookingStatus bookingStatus;
    private Long showId;
    private String movieName;
    private String hallName;
    private LocalDateTime showDateTime;
    private List<String> selectedSeatCodes;
    private BigDecimal totalAmount;
    private String currency;
    private LocalDateTime bookingTime;
    private LocalDateTime expiresAt;
}
