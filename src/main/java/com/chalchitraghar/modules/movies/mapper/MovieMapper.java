package com.chalchitraghar.modules.movies.mapper;

import org.springframework.stereotype.Component;

import com.chalchitraghar.modules.movies.dto.request.MovieRequest;
import com.chalchitraghar.modules.movies.dto.response.AdminMovieDetailResponse;
import com.chalchitraghar.modules.movies.dto.response.AdminMovieSummaryResponse;
import com.chalchitraghar.modules.movies.dto.response.MovieResponse;
import com.chalchitraghar.modules.movies.dto.response.PublicMovieDetailResponse;
import com.chalchitraghar.modules.movies.dto.response.PublicMovieSummaryResponse;
import com.chalchitraghar.modules.movies.entity.Movie;

/**
 * Mapper for converting between Movie entity and DTOs.
 */
@Component
public class MovieMapper {

    /**
     * Converts a MovieRequest DTO to a Movie entity.
     *
     * @param dto the request DTO to convert
     * @return the Movie entity, or null if dto is null
     */
    public Movie toEntity(MovieRequest dto) {
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

    /**
     * Updates an existing Movie entity with values from a MovieRequest DTO.
     *
     * @param movie the entity to update
     * @param dto the request DTO containing new values
     */
    public void updateEntityFromDto(Movie movie, MovieRequest dto) {
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

    /**
     * Converts a Movie entity to a MovieResponse DTO.
     *
     * @param movie the entity to convert
     * @return the response DTO, or null if movie is null
     */
    public MovieResponse toResponseDto(Movie movie) {
        if (movie == null) {
            return null;
        }

        MovieResponse dto = new MovieResponse();
        mapPublicDetailFields(movie, dto);
        mapAuditFields(movie, dto);
        return dto;
    }

    public PublicMovieSummaryResponse toPublicSummary(Movie movie) {
        if (movie == null) {
            return null;
        }

        PublicMovieSummaryResponse dto = new PublicMovieSummaryResponse();
        dto.setId(movie.getId());
        dto.setTitle(movie.getTitle());
        dto.setGenre(movie.getGenre());
        dto.setDurationMinutes(movie.getDurationMinutes());
        dto.setLanguage(movie.getLanguage());
        dto.setPosterUrl(movie.getPosterUrl());
        dto.setReleaseDate(movie.getReleaseDate());
        dto.setStatus(movie.getStatus());
        return dto;
    }

    public PublicMovieDetailResponse toPublicDetail(Movie movie) {
        if (movie == null) {
            return null;
        }

        PublicMovieDetailResponse dto = new PublicMovieDetailResponse();
        mapPublicDetailFields(movie, dto);
        return dto;
    }

    public AdminMovieSummaryResponse toAdminSummary(Movie movie) {
        if (movie == null) {
            return null;
        }

        AdminMovieSummaryResponse dto = new AdminMovieSummaryResponse();
        dto.setId(movie.getId());
        dto.setTitle(movie.getTitle());
        dto.setGenre(movie.getGenre());
        dto.setLanguage(movie.getLanguage());
        dto.setReleaseDate(movie.getReleaseDate());
        dto.setStatus(movie.getStatus());
        return dto;
    }

    public AdminMovieDetailResponse toAdminDetail(Movie movie) {
        if (movie == null) {
            return null;
        }

        AdminMovieDetailResponse dto = new AdminMovieDetailResponse();
        mapPublicDetailFields(movie, dto);
        mapAuditFields(movie, dto);
        return dto;
    }

    private void mapPublicDetailFields(Movie movie, PublicMovieDetailResponse dto) {
        dto.setId(movie.getId());
        dto.setTitle(movie.getTitle());
        dto.setGenre(movie.getGenre());
        dto.setDurationMinutes(movie.getDurationMinutes());
        dto.setLanguage(movie.getLanguage());
        dto.setDescription(movie.getDescription());
        dto.setPosterUrl(movie.getPosterUrl());
        dto.setReleaseDate(movie.getReleaseDate());
        dto.setStatus(movie.getStatus());
    }

    private void mapPublicDetailFields(Movie movie, AdminMovieDetailResponse dto) {
        dto.setId(movie.getId());
        dto.setTitle(movie.getTitle());
        dto.setGenre(movie.getGenre());
        dto.setDurationMinutes(movie.getDurationMinutes());
        dto.setLanguage(movie.getLanguage());
        dto.setDescription(movie.getDescription());
        dto.setPosterUrl(movie.getPosterUrl());
        dto.setReleaseDate(movie.getReleaseDate());
        dto.setStatus(movie.getStatus());
    }

    private void mapPublicDetailFields(Movie movie, MovieResponse dto) {
        dto.setId(movie.getId());
        dto.setTitle(movie.getTitle());
        dto.setGenre(movie.getGenre());
        dto.setDurationMinutes(movie.getDurationMinutes());
        dto.setLanguage(movie.getLanguage());
        dto.setDescription(movie.getDescription());
        dto.setPosterUrl(movie.getPosterUrl());
        dto.setReleaseDate(movie.getReleaseDate());
        dto.setStatus(movie.getStatus());
    }

    private void mapAuditFields(Movie movie, AdminMovieDetailResponse dto) {
        dto.setCreatedAt(movie.getCreatedAt());
        dto.setUpdatedAt(movie.getUpdatedAt());
    }

    private void mapAuditFields(Movie movie, MovieResponse dto) {
        dto.setCreatedAt(movie.getCreatedAt());
        dto.setUpdatedAt(movie.getUpdatedAt());
    }
}
