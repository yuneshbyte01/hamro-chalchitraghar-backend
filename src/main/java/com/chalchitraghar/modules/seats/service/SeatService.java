package com.chalchitraghar.modules.seats.service;

import java.util.List;

import com.chalchitraghar.modules.seats.dto.response.SeatResponse;

/**
 * Service for seat retrieval operations.
 */
public interface SeatService {

    List<SeatResponse> getAllSeatsForShow(Long showId);
}
