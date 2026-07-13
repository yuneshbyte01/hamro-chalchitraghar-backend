package com.chalchitraghar.modules.bookings.dto.response;

import com.chalchitraghar.modules.bookings.enums.BookingStatus;
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
@Schema(description = "Admin operational booking summary")
public class AdminBookingSummaryResponse {
    private Long bookingId;
    private String bookingReference;
    private BookingStatus bookingStatus;
    private Long customerId;
    private String customerName;
    private String customerEmail;
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
