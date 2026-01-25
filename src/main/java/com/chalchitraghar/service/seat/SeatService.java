package com.chalchitraghar.service.seat;

import java.util.List;

import com.chalchitraghar.dto.seat.SeatResponse;

/**
 * Service for seat retrieval operations.
 */
public interface SeatService {

    List<SeatResponse> getAllSeatsForShow(Long showId);
}
