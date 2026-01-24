package com.chalchitraghar.mapper;

import org.springframework.stereotype.Component;

import com.chalchitraghar.dto.show.ShowRequest;
import com.chalchitraghar.dto.show.ShowResponse;
import com.chalchitraghar.model.Hall;
import com.chalchitraghar.model.Movie;
import com.chalchitraghar.model.Show;

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

    /**
     * Converts a Show entity to a ShowResponse DTO.
     *
     * @param show the entity to convert
     * @return the response DTO, or null if show is null
     */
    public ShowResponse toResponseDto(Show show) {
        if (show == null) {
            return null;
        }

        ShowResponse dto = new ShowResponse();
        dto.setId(show.getId());
        dto.setMovie(movieMapper.toResponseDto(show.getMovie()));
        dto.setHall(hallMapper.toResponseDto(show.getHall()));
        dto.setStatus(show.getStatus());
        dto.setShowDate(show.getShowDate());
        dto.setShowTime(show.getShowTime());
        dto.setEndTime(show.getEndTime());
        dto.setCreatedAt(show.getCreatedAt());
        dto.setUpdatedAt(show.getUpdatedAt());
        return dto;
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
