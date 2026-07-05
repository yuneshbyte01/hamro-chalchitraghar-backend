package com.chalchitraghar.modules.movies.service.impl;

import com.chalchitraghar.modules.movies.service.MovieService;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chalchitraghar.modules.movies.dto.request.MovieRequest;
import com.chalchitraghar.modules.movies.dto.request.MovieSearchCriteria;
import com.chalchitraghar.modules.movies.dto.response.AdminMovieDetailResponse;
import com.chalchitraghar.modules.movies.dto.response.AdminMovieSummaryResponse;
import com.chalchitraghar.modules.movies.dto.response.PublicMovieDetailResponse;
import com.chalchitraghar.modules.movies.dto.response.PublicMovieSummaryResponse;
import com.chalchitraghar.shared.exception.ResourceNotFoundException;
import com.chalchitraghar.shared.response.PageResponse;
import com.chalchitraghar.modules.movies.mapper.MovieMapper;
import com.chalchitraghar.modules.movies.entity.Movie;
import com.chalchitraghar.modules.movies.enums.MovieStatus;
import com.chalchitraghar.modules.movies.repository.MovieRepository;
import com.chalchitraghar.modules.movies.specification.MovieSpecification;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class MovieServiceImpl implements MovieService {

    private static final List<String> MOVIE_SORT_FIELDS = List.of(
            "id",
            "title",
            "genre",
            "language",
            "releaseDate",
            "status",
            "createdAt",
            "updatedAt",
            "durationMinutes"
    );

    private final MovieRepository movieRepository;
    private final MovieMapper movieMapper;

    @Override
    public AdminMovieDetailResponse addMovie(MovieRequest dto) {
        Movie movie = movieMapper.toEntity(dto);
        return movieMapper.toAdminDetail(movieRepository.save(movie));
    }

    @Override
    public AdminMovieDetailResponse updateMovie(Long id, MovieRequest dto) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", id));
        movieMapper.updateEntityFromDto(movie, dto);
        return movieMapper.toAdminDetail(movieRepository.save(movie));
    }

    @Override
    public void deleteMovie(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", id));
        movie.setStatus(MovieStatus.ENDED);
        movieRepository.save(movie);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PublicMovieSummaryResponse> getPublicMovies(
            MovieSearchCriteria criteria,
            int page,
            int size,
            String sortBy,
            String sortDir) {
        var movies = searchMovies(criteria, page, size, sortBy, sortDir);
        List<PublicMovieSummaryResponse> content = movies.getContent().stream()
                .map(movieMapper::toPublicSummary)
                .toList();
        return PageResponse.from(movies, content);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicMovieDetailResponse getPublicMovieById(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", id));
        return movieMapper.toPublicDetail(movie);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicMovieSummaryResponse> getPublicMoviesByStatus(MovieStatus status) {
        return movieRepository.findAllByStatusOrderByReleaseDateAsc(status).stream()
                .map(movieMapper::toPublicSummary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminMovieSummaryResponse> getAdminMovies(
            MovieSearchCriteria criteria,
            int page,
            int size,
            String sortBy,
            String sortDir) {
        var movies = searchMovies(criteria, page, size, sortBy, sortDir);
        List<AdminMovieSummaryResponse> content = movies.getContent().stream()
                .map(movieMapper::toAdminSummary)
                .toList();
        return PageResponse.from(movies, content);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminMovieDetailResponse getAdminMovieById(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", id));
        return movieMapper.toAdminDetail(movie);
    }

    private org.springframework.data.domain.Page<Movie> searchMovies(
            MovieSearchCriteria criteria,
            int page,
            int size,
            String sortBy,
            String sortDir) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be zero or greater");
        }
        if (size < 1) {
            throw new IllegalArgumentException("Size must be at least 1");
        }
        if (!MOVIE_SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException("Invalid sortBy. Allowed values: "
                    + String.join(", ", MOVIE_SORT_FIELDS));
        }
        if (criteria.releaseDateFrom() != null
                && criteria.releaseDateTo() != null
                && criteria.releaseDateFrom().isAfter(criteria.releaseDateTo())) {
            throw new IllegalArgumentException("releaseDateFrom must be on or before releaseDateTo");
        }

        Sort.Direction direction = parseSortDirection(sortDir);
        MovieStatus status = parseEnum(MovieStatus.class, criteria.status(), "status");
        var pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return movieRepository.findAll(MovieSpecification.search(criteria, status), pageable);
    }

    private Sort.Direction parseSortDirection(String sortDir) {
        if ("asc".equalsIgnoreCase(sortDir)) {
            return Sort.Direction.ASC;
        }
        if ("desc".equalsIgnoreCase(sortDir)) {
            return Sort.Direction.DESC;
        }
        throw new IllegalArgumentException("Invalid sortDir. Allowed values: asc, desc");
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumType, String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Arrays.stream(enumType.getEnumConstants())
                .filter(enumValue -> enumValue.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid " + fieldName + ". Allowed values: " + allowedEnumValues(enumType)));
    }

    private <E extends Enum<E>> String allowedEnumValues(Class<E> enumType) {
        return String.join(", ", Arrays.stream(enumType.getEnumConstants())
                .map(Enum::name)
                .toList());
    }
}
