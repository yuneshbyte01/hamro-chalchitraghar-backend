package com.chalchitraghar.modules.halls.service;

import java.util.List;

import com.chalchitraghar.modules.halls.dto.request.HallRequest;
import com.chalchitraghar.modules.halls.dto.response.HallResponse;

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
