package com.chalchitraghar.service.show;

import java.time.LocalDate;
import java.util.List;

import com.chalchitraghar.dto.show.ShowRequest;
import com.chalchitraghar.dto.show.ShowResponse;

/**
 * Service for show management operations.
 */
public interface ShowService {

    ShowResponse addShow(ShowRequest dto);

    ShowResponse updateShow(Long id, ShowRequest dto);

    void deleteShow(Long id);

    List<ShowResponse> getAllShows();

    ShowResponse getShowById(Long id);

    List<ShowResponse> getShowsByHall(Long hallId);

    List<ShowResponse> getShowsByMovie(Long movieId);

    List<ShowResponse> getShowsByMovieAndShowDate(Long movieId, LocalDate showDate);
}
