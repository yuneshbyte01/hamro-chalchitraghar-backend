package com.chalchitraghar.modules.halls.service;

import java.util.List;

import com.chalchitraghar.modules.halls.dto.request.HallRequest;
import com.chalchitraghar.modules.halls.dto.response.AdminHallDetailResponse;
import com.chalchitraghar.modules.halls.dto.response.AdminHallSummaryResponse;
import com.chalchitraghar.modules.halls.dto.response.PublicHallDetailResponse;
import com.chalchitraghar.modules.halls.dto.response.PublicHallSummaryResponse;

/**
 * Service for hall management operations.
 */
public interface HallService {

    AdminHallDetailResponse addHall(HallRequest dto);

    AdminHallDetailResponse updateHall(Long id, HallRequest dto);

    void deleteHall(Long id);

    List<PublicHallSummaryResponse> getPublicHalls();

    PublicHallDetailResponse getPublicHallById(Long id);

    List<PublicHallSummaryResponse> getPublicActiveHalls();

    List<AdminHallSummaryResponse> getAdminHalls();

    AdminHallDetailResponse getAdminHallById(Long id);

    List<AdminHallSummaryResponse> getAdminActiveHalls();
}
