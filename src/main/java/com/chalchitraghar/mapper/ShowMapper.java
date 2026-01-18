package com.chalchitraghar.mapper;

import org.springframework.stereotype.Component;

import com.chalchitraghar.dto.show.ShowRequestDto;
import com.chalchitraghar.dto.show.ShowResponseDto;
import com.chalchitraghar.model.Show;
import com.chalchitraghar.model.Movie;
import com.chalchitraghar.model.Hall;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ShowMapper {

    private final MovieMapper movieMapper;
    private final HallMapper hallMapper;

    public Show toEntity(ShowRequestDto dto, Movie movie, Hall hall) {
        if (dto == null) {
            return null;
        }

        return Show.builder()
                .movie(movie)
                .hall(hall)
                .price(dto.getPrice())
                .showDate(dto.getShowDate())
                .showTime(dto.getShowTime())
                .endTime(dto.getEndTime())
                .build();
    }

    public ShowResponseDto toResponseDto(Show show) {
        if (show == null) {
            return null;
        }

        ShowResponseDto dto = new ShowResponseDto();
        dto.setId(show.getId());
        dto.setMovie(movieMapper.toResponseDto(show.getMovie()));
        dto.setHall(hallMapper.toResponseDto(show.getHall()));
        dto.setPrice(show.getPrice());
        dto.setStatus(show.getStatus());
        dto.setShowDate(show.getShowDate());
        dto.setShowTime(show.getShowTime());
        dto.setEndTime(show.getEndTime());
        dto.setCreatedAt(show.getCreatedAt());
        dto.setUpdatedAt(show.getUpdatedAt());
        return dto;
    }

    public void updateEntityFromDto(Show show, ShowRequestDto dto, Movie movie, Hall hall) {
        if (show == null || dto == null) {
            return;
        }

        show.setMovie(movie);
        show.setHall(hall);
        show.setPrice(dto.getPrice());
        show.setShowDate(dto.getShowDate());
        show.setShowTime(dto.getShowTime());
        show.setEndTime(dto.getEndTime());
    }
}
