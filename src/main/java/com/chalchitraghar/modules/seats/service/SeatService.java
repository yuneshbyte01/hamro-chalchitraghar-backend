package com.chalchitraghar.modules.seats.service;

import com.chalchitraghar.modules.seats.dto.response.SeatResponse;
import java.util.List;

/** Service for seat retrieval operations. */
public interface SeatService {

    List<SeatResponse> getAllSeatsForShow(Long showId);
}
