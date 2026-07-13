package com.chalchitraghar.modules.shows.service;

import com.chalchitraghar.modules.shows.dto.request.ShowRequest;
import com.chalchitraghar.modules.shows.dto.request.ShowSearchCriteria;
import com.chalchitraghar.modules.shows.dto.response.AdminShowDetailResponse;
import com.chalchitraghar.modules.shows.dto.response.AdminShowSummaryResponse;
import com.chalchitraghar.modules.shows.dto.response.PublicShowDetailResponse;
import com.chalchitraghar.modules.shows.dto.response.PublicShowSummaryResponse;
import com.chalchitraghar.shared.response.PageResponse;
import java.time.LocalDate;
import java.util.List;

/** Service for show management operations. */
public interface ShowService {

    AdminShowDetailResponse addShow(ShowRequest dto);

    AdminShowDetailResponse updateShow(Long id, ShowRequest dto);

    AdminShowDetailResponse updateShowStatus(
            Long id, com.chalchitraghar.modules.shows.enums.ShowStatus status);

    void deleteShow(Long id);

    PageResponse<PublicShowSummaryResponse> getPublicShows(
            ShowSearchCriteria criteria, int page, int size, String sortBy, String sortDir);

    PublicShowDetailResponse getPublicShowById(Long id);

    List<PublicShowSummaryResponse> getPublicShowsByMovie(Long movieId);

    List<PublicShowSummaryResponse> getPublicShowsByMovieAndShowDate(
            Long movieId, LocalDate showDate);

    PageResponse<AdminShowSummaryResponse> getAdminShows(
            ShowSearchCriteria criteria, int page, int size, String sortBy, String sortDir);

    AdminShowDetailResponse getAdminShowById(Long id);
}
