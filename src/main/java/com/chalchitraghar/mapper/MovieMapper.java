package com.chalchitraghar.mapper;

import org.springframework.stereotype.Component;

import com.chalchitraghar.dto.MovieDto;
import com.chalchitraghar.dto.MovieResponseDto;
import com.chalchitraghar.model.Movie;

@Component
public class MovieMapper {

    public Movie toEntity(MovieDto dto) {
        if (dto == null) {
            return null;
        }

        return Movie.builder()
                .title(dto.getTitle())
                .genre(dto.getGenre())
                .durationMinutes(dto.getDurationMinutes())
                .language(dto.getLanguage())
                .posterUrl(dto.getPosterUrl())
                .description(dto.getDescription())
                .status(dto.getStatus())
                .releaseDate(dto.getReleaseDate())
                .build();
    }

    public void updateEntityFromDto(Movie movie, MovieDto dto) {
        if (movie == null || dto == null) {
            return;
        }

        movie.setTitle(dto.getTitle());
        movie.setGenre(dto.getGenre());
        movie.setDurationMinutes(dto.getDurationMinutes());
        movie.setLanguage(dto.getLanguage());
        movie.setPosterUrl(dto.getPosterUrl());
        movie.setDescription(dto.getDescription());
        movie.setStatus(dto.getStatus());
        movie.setReleaseDate(dto.getReleaseDate());
    }

    public MovieResponseDto toResponseDto(Movie movie) {
        if (movie == null) {
            return null;
        }

        MovieResponseDto dto = new MovieResponseDto();
        dto.setId(movie.getId());
        dto.setTitle(movie.getTitle());
        dto.setGenre(movie.getGenre());
        dto.setDurationMinutes(movie.getDurationMinutes());
        dto.setLanguage(movie.getLanguage());
        dto.setDescription(movie.getDescription());
        dto.setPosterUrl(movie.getPosterUrl());
        dto.setReleaseDate(movie.getReleaseDate());
        dto.setStatus(movie.getStatus());
        dto.setCreatedAt(movie.getCreatedAt());
        dto.setUpdatedAt(movie.getUpdatedAt());
        return dto;
    }
}
