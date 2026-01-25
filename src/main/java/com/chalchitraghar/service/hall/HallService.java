package com.chalchitraghar.service.hall;

import java.util.List;

import com.chalchitraghar.dto.hall.HallRequest;
import com.chalchitraghar.dto.hall.HallResponse;

/**
 * Service for hall management operations.
 */
public interface HallService {

    HallResponse addHall(HallRequest dto);

    HallResponse updateHall(Long id, HallRequest dto);

    void deleteHall(Long id);

    List<HallResponse> getAllHalls();

    HallResponse getHallById(Long id);

    List<HallResponse> getActiveHalls();
}
