package com.chalchitraghar.modules.shows.mapper;

import com.chalchitraghar.modules.halls.mapper.HallMapper;

import com.chalchitraghar.modules.movies.mapper.MovieMapper;

import org.springframework.stereotype.Component;

import com.chalchitraghar.modules.shows.dto.request.ShowRequest;
import com.chalchitraghar.modules.shows.dto.response.AdminShowDetailResponse;
import com.chalchitraghar.modules.shows.dto.response.AdminShowSummaryResponse;
import com.chalchitraghar.modules.shows.dto.response.PublicShowDetailResponse;
import com.chalchitraghar.modules.shows.dto.response.PublicShowSummaryResponse;
import com.chalchitraghar.modules.halls.entity.Hall;
import com.chalchitraghar.modules.movies.entity.Movie;
import com.chalchitraghar.modules.shows.entity.Show;

import lombok.RequiredArgsConstructor;

/**
 * Mapper for converting between Show entity and DTOs.
 * Handles nested mapping of associated Movie and Hall entities.
 */
@Component
@RequiredArgsConstructor
public class ShowMapper {

    private final MovieMapper movieMapper;
    private final HallMapper hallMapper;

    /**
     * Converts a ShowRequest DTO to a Show entity.
     *
     * @param dto the request DTO to convert
     * @param movie the Movie entity to associate
     * @param hall the Hall entity to associate
     * @return the Show entity, or null if dto is null
     */
    public Show toEntity(ShowRequest dto, Movie movie, Hall hall) {
        if (dto == null) {
            return null;
        }

        return Show.builder()
                .movie(movie)
                .hall(hall)
                .showDate(dto.getShowDate())
                .showTime(dto.getShowTime())
                .endTime(dto.getEndTime())
                .build();
    }

    /** Converts a show to its public list representation. */
    public PublicShowSummaryResponse toPublicSummary(Show show) {
        if (show == null) {
            return null;
        }
        PublicShowSummaryResponse dto = new PublicShowSummaryResponse();
        mapSummary(show, dto);
        return dto;
    }

    public PublicShowDetailResponse toPublicDetail(Show show) {
        if (show == null) {
            return null;
        }
        PublicShowDetailResponse dto = new PublicShowDetailResponse();
        dto.setId(show.getId());
        dto.setMovie(movieMapper.toPublicDetail(show.getMovie()));
        dto.setHall(hallMapper.toPublicDetail(show.getHall()));
        dto.setStatus(show.getStatus());
        dto.setShowDate(show.getShowDate());
        dto.setShowTime(show.getShowTime());
        dto.setEndTime(show.getEndTime());
        return dto;
    }

    public AdminShowSummaryResponse toAdminSummary(Show show) {
        if (show == null) {
            return null;
        }
        AdminShowSummaryResponse dto = new AdminShowSummaryResponse();
        mapSummary(show, dto);
        return dto;
    }

    public AdminShowDetailResponse toAdminDetail(Show show) {
        if (show == null) {
            return null;
        }
        AdminShowDetailResponse dto = new AdminShowDetailResponse();
        dto.setId(show.getId());
        dto.setMovie(movieMapper.toAdminDetail(show.getMovie()));
        dto.setHall(hallMapper.toAdminDetail(show.getHall()));
        dto.setStatus(show.getStatus());
        dto.setShowDate(show.getShowDate());
        dto.setShowTime(show.getShowTime());
        dto.setEndTime(show.getEndTime());
        dto.setCreatedAt(show.getCreatedAt());
        dto.setUpdatedAt(show.getUpdatedAt());
        return dto;
    }

    private void mapSummary(Show show, PublicShowSummaryResponse dto) {
        dto.setId(show.getId());
        dto.setMovieId(show.getMovie().getId());
        dto.setMovieTitle(show.getMovie().getTitle());
        dto.setHallId(show.getHall().getId());
        dto.setHallName(show.getHall().getName());
        dto.setStatus(show.getStatus());
        dto.setShowDate(show.getShowDate());
        dto.setShowTime(show.getShowTime());
        dto.setEndTime(show.getEndTime());
    }

    private void mapSummary(Show show, AdminShowSummaryResponse dto) {
        dto.setId(show.getId());
        dto.setMovieId(show.getMovie().getId());
        dto.setMovieTitle(show.getMovie().getTitle());
        dto.setHallId(show.getHall().getId());
        dto.setHallName(show.getHall().getName());
        dto.setStatus(show.getStatus());
        dto.setShowDate(show.getShowDate());
        dto.setShowTime(show.getShowTime());
        dto.setEndTime(show.getEndTime());
    }

    /**
     * Updates an existing Show entity with values from a ShowRequest DTO.
     *
     * @param show the entity to update
     * @param dto the request DTO containing new values
     * @param movie the Movie entity to associate
     * @param hall the Hall entity to associate
     */
    public void updateEntityFromDto(Show show, ShowRequest dto, Movie movie, Hall hall) {
        if (show == null || dto == null) {
            return;
        }

        show.setMovie(movie);
        show.setHall(hall);
        show.setShowDate(dto.getShowDate());
        show.setShowTime(dto.getShowTime());
        show.setEndTime(dto.getEndTime());
    }
}
