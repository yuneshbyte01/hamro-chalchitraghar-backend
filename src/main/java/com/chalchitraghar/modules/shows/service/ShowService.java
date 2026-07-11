package com.chalchitraghar.modules.shows.service;

import java.time.LocalDate;
import java.util.List;

import com.chalchitraghar.modules.shows.dto.request.ShowRequest;
import com.chalchitraghar.modules.shows.dto.response.AdminShowDetailResponse;
import com.chalchitraghar.modules.shows.dto.response.AdminShowSummaryResponse;
import com.chalchitraghar.modules.shows.dto.response.PublicShowDetailResponse;
import com.chalchitraghar.modules.shows.dto.response.PublicShowSummaryResponse;

/**
 * Service for show management operations.
 */
public interface ShowService {

    AdminShowDetailResponse addShow(ShowRequest dto);

    AdminShowDetailResponse updateShow(Long id, ShowRequest dto);

    void deleteShow(Long id);

    List<PublicShowSummaryResponse> getPublicShows();

    PublicShowDetailResponse getPublicShowById(Long id);

    List<PublicShowSummaryResponse> getPublicShowsByMovie(Long movieId);

    List<PublicShowSummaryResponse> getPublicShowsByMovieAndShowDate(Long movieId, LocalDate showDate);

    List<AdminShowSummaryResponse> getAdminShows();

    AdminShowDetailResponse getAdminShowById(Long id);
}
