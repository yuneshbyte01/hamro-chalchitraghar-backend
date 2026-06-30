package com.chalchitraghar.modules.seats;

import java.util.List;

import com.chalchitraghar.modules.seats.SeatResponse;

/**
 * Service for seat retrieval operations.
 */
public interface SeatService {

    List<SeatResponse> getAllSeatsForShow(Long showId);
}
