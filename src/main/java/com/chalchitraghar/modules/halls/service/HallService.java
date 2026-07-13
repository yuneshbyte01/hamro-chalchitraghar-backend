package com.chalchitraghar.modules.halls.service;

import com.chalchitraghar.modules.halls.dto.request.HallRequest;
import com.chalchitraghar.modules.halls.dto.request.HallSearchCriteria;
import com.chalchitraghar.modules.halls.dto.response.AdminHallDetailResponse;
import com.chalchitraghar.modules.halls.dto.response.AdminHallSummaryResponse;
import com.chalchitraghar.modules.halls.dto.response.PublicHallDetailResponse;
import com.chalchitraghar.modules.halls.dto.response.PublicHallSummaryResponse;
import com.chalchitraghar.shared.response.PageResponse;
import java.util.List;

/** Service for hall management operations. */
public interface HallService {

    AdminHallDetailResponse addHall(HallRequest dto);

    AdminHallDetailResponse updateHall(Long id, HallRequest dto);

    void deleteHall(Long id);

    PageResponse<PublicHallSummaryResponse> getPublicHalls(
            HallSearchCriteria criteria, int page, int size, String sortBy, String sortDir);

    PublicHallDetailResponse getPublicHallById(Long id);

    List<PublicHallSummaryResponse> getPublicActiveHalls();

    PageResponse<AdminHallSummaryResponse> getAdminHalls(
            HallSearchCriteria criteria, int page, int size, String sortBy, String sortDir);

    AdminHallDetailResponse getAdminHallById(Long id);

    List<AdminHallSummaryResponse> getAdminActiveHalls();
}
