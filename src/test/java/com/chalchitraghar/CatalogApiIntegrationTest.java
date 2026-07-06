package com.chalchitraghar;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.chalchitraghar.modules.halls.entity.Hall;
import com.chalchitraghar.modules.halls.enums.Status;
import com.chalchitraghar.modules.movies.entity.Movie;
import com.chalchitraghar.modules.movies.enums.MovieStatus;
import com.chalchitraghar.modules.shows.entity.Show;
import com.chalchitraghar.modules.shows.enums.ShowStatus;
import com.chalchitraghar.modules.users.enums.Role;

class CatalogApiIntegrationTest extends AbstractIntegrationTest {

    @Test
    void publicMovieListReturnsApiResponse() throws Exception {
        saveMovie("Public Movie", MovieStatus.NOW_SHOWING);

        mockMvc.perform(get("/api/public/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Movies fetched successfully"))
                .andExpect(jsonPath("$.data.content[0].title").value("Public Movie"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].description").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].updatedAt").doesNotExist())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void publicMovieListExcludesEndedMoviesByDefault() throws Exception {
        saveMovie("Public Current", MovieStatus.NOW_SHOWING, "Drama", "Nepali", LocalDate.now());
        saveMovie("Public Upcoming", MovieStatus.UPCOMING, "Drama", "Nepali", LocalDate.now().plusDays(10));
        saveMovie("Public Ended", MovieStatus.ENDED, "Drama", "Nepali", LocalDate.now().minusDays(10));

        mockMvc.perform(get("/api/public/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].title").value("Public Current"))
                .andExpect(jsonPath("$.data.content[1].title").value("Public Upcoming"));
    }

    @Test
    void publicMovieListSupportsPageAndSize() throws Exception {
        saveMovie("Movie One", MovieStatus.NOW_SHOWING, "Drama", "Nepali", LocalDate.of(2026, 1, 1));
        saveMovie("Movie Two", MovieStatus.NOW_SHOWING, "Drama", "Nepali", LocalDate.of(2026, 1, 2));
        saveMovie("Movie Three", MovieStatus.NOW_SHOWING, "Drama", "Nepali", LocalDate.of(2026, 1, 3));

        mockMvc.perform(get("/api/public/movies")
                        .param("page", "1")
                        .param("size", "1")
                        .param("sortBy", "releaseDate")
                        .param("sortDir", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("Movie Two"))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.last").value(false));
    }

    @Test
    void publicMovieListSupportsSearchFiltersAndSorting() throws Exception {
        saveMovie("Jatra Returns", MovieStatus.NOW_SHOWING, "Comedy", "Nepali", LocalDate.of(2026, 5, 10));
        saveMovie("Silent Hills", MovieStatus.UPCOMING, "Horror", "English", LocalDate.of(2026, 7, 1));
        saveMovie("Old Jatra", MovieStatus.ENDED, "Comedy", "Nepali", LocalDate.of(2025, 1, 1));

        mockMvc.perform(get("/api/public/movies").param("search", "jatra"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(get("/api/public/movies").param("search", "horror"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Silent Hills"));

        mockMvc.perform(get("/api/public/movies").param("status", "NOW_SHOWING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Jatra Returns"));

        mockMvc.perform(get("/api/public/movies").param("genre", "Comedy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(get("/api/public/movies").param("language", "Nepali"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(get("/api/public/movies")
                        .param("releaseDateFrom", "2026-01-01")
                        .param("releaseDateTo", "2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));

        mockMvc.perform(get("/api/public/movies")
                        .param("search", "jatra")
                        .param("status", "NOW_SHOWING")
                        .param("language", "Nepali"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Jatra Returns"));

        mockMvc.perform(get("/api/public/movies")
                        .param("sortBy", "releaseDate")
                        .param("sortDir", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("Jatra Returns"))
                .andExpect(jsonPath("$.data.content[1].title").value("Silent Hills"));
    }

    @Test
    void publicMovieListRejectsInvalidQueryParameters() throws Exception {
        mockMvc.perform(get("/api/public/movies").param("sortBy", "password"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid sortBy. Allowed values: id, title, genre, language, releaseDate, status, createdAt, updatedAt, durationMinutes"));

        mockMvc.perform(get("/api/public/movies").param("sortDir", "sideways"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid sortDir. Allowed values: asc, desc"));

        mockMvc.perform(get("/api/public/movies").param("status", "PLAYING"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid status. Allowed values: UPCOMING, NOW_SHOWING, ENDED"));

        mockMvc.perform(get("/api/public/movies").param("status", "ENDED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Public movie listing does not support status ENDED"));

        mockMvc.perform(get("/api/public/movies").param("releaseDateFrom", "2026/01/01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Parameter 'releaseDateFrom' has invalid type"));

        mockMvc.perform(get("/api/public/movies")
                        .param("releaseDateFrom", "2026-12-31")
                        .param("releaseDateTo", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("releaseDateFrom must be on or before releaseDateTo"));
    }

    @Test
    void publicMovieDetailDoesNotExposeAuditFields() throws Exception {
        Movie movie = saveMovie("Public Movie Detail", MovieStatus.NOW_SHOWING);

        mockMvc.perform(get("/api/public/movies/{id}", movie.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Movie fetched successfully"))
                .andExpect(jsonPath("$.data.title").value("Public Movie Detail"))
                .andExpect(jsonPath("$.data.description").value("Test movie"))
                .andExpect(jsonPath("$.data.createdAt").doesNotExist())
                .andExpect(jsonPath("$.data.updatedAt").doesNotExist())
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.errors").doesNotExist());
    }

    @Test
    void publicMovieDetailForEndedMovieReturnsNotFound() throws Exception {
        Movie movie = saveMovie("Hidden Public Detail", MovieStatus.ENDED, "Drama", "Nepali", LocalDate.now().minusDays(10));

        mockMvc.perform(get("/api/public/movies/{id}", movie.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Movie not found with id: " + movie.getId()));
    }

    @Test
    void adminMovieListUsesSummaryContract() throws Exception {
        String adminToken = tokenFor("admin-movie-list@example.com", Role.ADMIN);
        saveMovie("Admin Movie List", MovieStatus.UPCOMING);

        mockMvc.perform(get("/api/admin/movies")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Movies fetched successfully"))
                .andExpect(jsonPath("$.data.content[0].title").value("Admin Movie List"))
                .andExpect(jsonPath("$.data.content[0].genre").value("Drama"))
                .andExpect(jsonPath("$.data.content[0].language").value("Nepali"))
                .andExpect(jsonPath("$.data.content[0].durationMinutes").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].description").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].posterUrl").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].updatedAt").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].password").doesNotExist())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void adminMovieListSupportsSearchStatusAndCreatedAtSorting() throws Exception {
        String adminToken = tokenFor("admin-movie-search@example.com", Role.ADMIN);
        saveMovie("First Admin Movie", MovieStatus.NOW_SHOWING, "Drama", "Nepali", LocalDate.of(2026, 4, 1));
        Thread.sleep(20);
        saveMovie("Ended Admin Movie", MovieStatus.ENDED, "Comedy", "English", LocalDate.of(2026, 3, 1));

        mockMvc.perform(get("/api/admin/movies")
                        .header("Authorization", bearer(adminToken))
                        .param("search", "ended"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Ended Admin Movie"));

        mockMvc.perform(get("/api/admin/movies")
                        .header("Authorization", bearer(adminToken))
                        .param("status", "ENDED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("ENDED"));

        mockMvc.perform(get("/api/admin/movies")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("Ended Admin Movie"))
                .andExpect(jsonPath("$.data.content[1].title").value("First Admin Movie"));
    }

    @Test
    void adminCanListAndFetchEndedMovies() throws Exception {
        String adminToken = tokenFor("admin-ended-visibility@example.com", Role.ADMIN);
        Movie ended = saveMovie("Admin Visible Ended", MovieStatus.ENDED, "Drama", "Nepali", LocalDate.now().minusDays(10));

        mockMvc.perform(get("/api/admin/movies")
                        .header("Authorization", bearer(adminToken))
                        .param("status", "ENDED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Admin Visible Ended"));

        mockMvc.perform(get("/api/admin/movies/{id}", ended.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Admin Visible Ended"))
                .andExpect(jsonPath("$.data.status").value("ENDED"));
    }

    @Test
    void adminMovieDetailExposesAuditFields() throws Exception {
        String adminToken = tokenFor("admin-movie-detail@example.com", Role.ADMIN);
        Movie movie = saveMovie("Admin Movie Detail", MovieStatus.NOW_SHOWING);

        mockMvc.perform(get("/api/admin/movies/{id}", movie.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Movie fetched successfully"))
                .andExpect(jsonPath("$.data.title").value("Admin Movie Detail"))
                .andExpect(jsonPath("$.data.description").value("Test movie"))
                .andExpect(jsonPath("$.data.posterUrl").value("https://example.com/poster.jpg"))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.updatedAt").exists())
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void adminCanCreateMovie() throws Exception {
        String adminToken = tokenFor("admin-movie@example.com", Role.ADMIN);

        mockMvc.perform(post("/api/admin/movies")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(movieRequest("Admin Movie", MovieStatus.NOW_SHOWING))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Movie created successfully"))
                .andExpect(jsonPath("$.data.title").value("Admin Movie"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void duplicateMovieTitleAndReleaseDateReturnsConflict() throws Exception {
        String adminToken = tokenFor("admin-duplicate-movie@example.com", Role.ADMIN);
        saveMovie("Duplicate Movie", MovieStatus.NOW_SHOWING, "Drama", "Nepali", LocalDate.now());

        mockMvc.perform(post("/api/admin/movies")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(movieRequest(
                                "duplicate movie",
                                MovieStatus.NOW_SHOWING,
                                120,
                                "https://example.com/poster.jpg",
                                LocalDate.now()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Movie already exists with the same title and release date"));
    }

    @Test
    void sameTitleWithDifferentReleaseDateIsAllowed() throws Exception {
        String adminToken = tokenFor("admin-same-title-movie@example.com", Role.ADMIN);

        mockMvc.perform(post("/api/admin/movies")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(movieRequest(
                                "Same Title",
                                MovieStatus.NOW_SHOWING,
                                120,
                                "https://example.com/poster-one.jpg",
                                LocalDate.now()))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/admin/movies")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(movieRequest(
                                "Same Title",
                                MovieStatus.ENDED,
                                120,
                                "https://example.com/poster-two.jpg",
                                LocalDate.now().minusDays(1)))))
                .andExpect(status().isCreated());
    }

    @Test
    void moviePosterUrlValidationAllowsHttpAndHttpsOnly() throws Exception {
        String adminToken = tokenFor("admin-poster-validation@example.com", Role.ADMIN);

        mockMvc.perform(post("/api/admin/movies")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(movieRequest(
                                "HTTPS Poster",
                                MovieStatus.NOW_SHOWING,
                                120,
                                "https://example.com/poster.jpg",
                                LocalDate.now()))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/admin/movies")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(movieRequest(
                                "HTTP Poster",
                                MovieStatus.NOW_SHOWING,
                                120,
                                "http://example.com/poster.jpg",
                                LocalDate.now()))))
                .andExpect(status().isCreated());

        assertInvalidMovieRequest(adminToken, movieRequest(
                "FTP Poster",
                MovieStatus.NOW_SHOWING,
                120,
                "ftp://example.com/poster.jpg",
                LocalDate.now()));
        assertInvalidMovieRequest(adminToken, movieRequest(
                "JavaScript Poster",
                MovieStatus.NOW_SHOWING,
                120,
                "javascript:alert(1)",
                LocalDate.now()));
        assertInvalidMovieRequest(adminToken, movieRequest(
                "Malformed Poster",
                MovieStatus.NOW_SHOWING,
                120,
                "https://exa mple.com/poster.jpg",
                LocalDate.now()));
        assertInvalidMovieRequest(adminToken, movieRequest(
                "Long Poster",
                MovieStatus.NOW_SHOWING,
                120,
                "https://example.com/" + "a".repeat(490),
                LocalDate.now()));
    }

    @Test
    void movieDurationValidationEnforcesBounds() throws Exception {
        String adminToken = tokenFor("admin-duration-validation@example.com", Role.ADMIN);

        assertInvalidMovieRequest(adminToken, movieRequest(
                "Zero Duration",
                MovieStatus.NOW_SHOWING,
                0,
                "https://example.com/poster.jpg",
                LocalDate.now()));
        assertInvalidMovieRequest(adminToken, movieRequest(
                "Negative Duration",
                MovieStatus.NOW_SHOWING,
                -1,
                "https://example.com/poster.jpg",
                LocalDate.now()));
        assertInvalidMovieRequest(adminToken, movieRequest(
                "Too Long Duration",
                MovieStatus.NOW_SHOWING,
                601,
                "https://example.com/poster.jpg",
                LocalDate.now()));

        mockMvc.perform(post("/api/admin/movies")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(movieRequest(
                                "Valid Duration",
                                MovieStatus.NOW_SHOWING,
                                600,
                                "https://example.com/poster.jpg",
                                LocalDate.now()))))
                .andExpect(status().isCreated());
    }

    @Test
    void movieReleaseDateMustMatchStatus() throws Exception {
        String adminToken = tokenFor("admin-release-validation@example.com", Role.ADMIN);

        mockMvc.perform(post("/api/admin/movies")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(movieRequest(
                                "Upcoming Future",
                                MovieStatus.UPCOMING,
                                120,
                                "https://example.com/poster.jpg",
                                LocalDate.now().plusDays(10)))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/admin/movies")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(movieRequest(
                                "Upcoming Past",
                                MovieStatus.UPCOMING,
                                120,
                                "https://example.com/poster.jpg",
                                LocalDate.now().minusDays(1)))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("UPCOMING movies must have a release date today or in the future"));

        mockMvc.perform(post("/api/admin/movies")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(movieRequest(
                                "Now Showing Today",
                                MovieStatus.NOW_SHOWING,
                                120,
                                "https://example.com/poster.jpg",
                                LocalDate.now()))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/admin/movies")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(movieRequest(
                                "Now Showing Future",
                                MovieStatus.NOW_SHOWING,
                                120,
                                "https://example.com/poster.jpg",
                                LocalDate.now().plusDays(1)))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("NOW_SHOWING movies must have a release date today or in the past"));

        mockMvc.perform(post("/api/admin/movies")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(movieRequest(
                                "Ended Future",
                                MovieStatus.ENDED,
                                120,
                                "https://example.com/poster.jpg",
                                LocalDate.now().plusDays(1)))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("ENDED movies must have a release date today or in the past"));
    }

    @Test
    void movieStatusTransitionsFollowLifecycle() throws Exception {
        String adminToken = tokenFor("admin-movie-transitions@example.com", Role.ADMIN);

        Movie upcomingToNowShowing = saveMovie(
                "Upcoming To Now Showing",
                MovieStatus.UPCOMING,
                "Drama",
                "Nepali",
                LocalDate.now().plusDays(5));
        mockMvc.perform(put("/api/admin/movies/{id}", upcomingToNowShowing.getId())
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(movieRequest(
                                "Upcoming To Now Showing",
                                MovieStatus.NOW_SHOWING,
                                120,
                                "https://example.com/poster.jpg",
                                LocalDate.now()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("NOW_SHOWING"));

        Movie nowShowingToEnded = saveMovie(
                "Now Showing To Ended",
                MovieStatus.NOW_SHOWING,
                "Drama",
                "Nepali",
                LocalDate.now().minusDays(1));
        mockMvc.perform(put("/api/admin/movies/{id}", nowShowingToEnded.getId())
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(movieRequest(
                                "Now Showing To Ended",
                                MovieStatus.ENDED,
                                120,
                                "https://example.com/poster.jpg",
                                LocalDate.now().minusDays(1)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ENDED"));

        Movie upcomingToEnded = saveMovie(
                "Upcoming To Ended",
                MovieStatus.UPCOMING,
                "Drama",
                "Nepali",
                LocalDate.now().plusDays(5));
        mockMvc.perform(put("/api/admin/movies/{id}", upcomingToEnded.getId())
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(movieRequest(
                                "Upcoming To Ended",
                                MovieStatus.ENDED,
                                120,
                                "https://example.com/poster.jpg",
                                LocalDate.now().minusDays(1)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ENDED"));

        Movie endedToNowShowing = saveMovie(
                "Ended To Now Showing",
                MovieStatus.ENDED,
                "Drama",
                "Nepali",
                LocalDate.now().minusDays(1));
        mockMvc.perform(put("/api/admin/movies/{id}", endedToNowShowing.getId())
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(movieRequest(
                                "Ended To Now Showing",
                                MovieStatus.NOW_SHOWING,
                                120,
                                "https://example.com/poster.jpg",
                                LocalDate.now().minusDays(1)))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Invalid movie status transition from ENDED to NOW_SHOWING"));

        Movie endedToUpcoming = saveMovie(
                "Ended To Upcoming",
                MovieStatus.ENDED,
                "Drama",
                "Nepali",
                LocalDate.now().minusDays(1));
        mockMvc.perform(put("/api/admin/movies/{id}", endedToUpcoming.getId())
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(movieRequest(
                                "Ended To Upcoming",
                                MovieStatus.UPCOMING,
                                120,
                                "https://example.com/poster.jpg",
                                LocalDate.now().plusDays(1)))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Invalid movie status transition from ENDED to UPCOMING"));

        Movie nowShowingToUpcoming = saveMovie(
                "Now Showing To Upcoming",
                MovieStatus.NOW_SHOWING,
                "Drama",
                "Nepali",
                LocalDate.now().minusDays(1));
        mockMvc.perform(put("/api/admin/movies/{id}", nowShowingToUpcoming.getId())
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(movieRequest(
                                "Now Showing To Upcoming",
                                MovieStatus.UPCOMING,
                                120,
                                "https://example.com/poster.jpg",
                                LocalDate.now().plusDays(1)))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Invalid movie status transition from NOW_SHOWING to UPCOMING"));
    }

    @Test
    void deleteMovieWithoutFutureActiveShowsSetsStatusEnded() throws Exception {
        String adminToken = tokenFor("admin-delete-movie@example.com", Role.ADMIN);
        Movie movie = saveMovie("Delete Allowed Movie", MovieStatus.NOW_SHOWING, "Drama", "Nepali", LocalDate.now());

        mockMvc.perform(delete("/api/admin/movies/{id}", movie.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/admin/movies/{id}", movie.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ENDED"));
    }

    @Test
    void deleteMovieWithFutureScheduledShowReturnsConflict() throws Exception {
        String adminToken = tokenFor("admin-delete-future-show@example.com", Role.ADMIN);
        Movie movie = saveMovie("Delete Blocked Movie", MovieStatus.NOW_SHOWING, "Drama", "Nepali", LocalDate.now());
        Hall hall = saveHall("Delete Block Hall", Status.ACTIVE);
        saveShow(movie, hall, LocalDate.now().plusDays(2), ShowStatus.SCHEDULED);

        mockMvc.perform(delete("/api/admin/movies/{id}", movie.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot end movie while future active shows exist"));
    }

    @Test
    void deleteMovieAllowsPastCompletedAndCancelledShows() throws Exception {
        String adminToken = tokenFor("admin-delete-nonactive-show@example.com", Role.ADMIN);

        Movie completedMovie = saveMovie("Delete Completed Movie", MovieStatus.NOW_SHOWING, "Drama", "Nepali", LocalDate.now());
        Hall completedHall = saveHall("Completed Show Hall", Status.ACTIVE);
        saveShow(completedMovie, completedHall, LocalDate.now().minusDays(2), ShowStatus.COMPLETED);

        mockMvc.perform(delete("/api/admin/movies/{id}", completedMovie.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());

        Movie cancelledMovie = saveMovie("Delete Cancelled Movie", MovieStatus.NOW_SHOWING, "Drama", "Nepali", LocalDate.now());
        Hall cancelledHall = saveHall("Cancelled Show Hall", Status.ACTIVE);
        saveShow(cancelledMovie, cancelledHall, LocalDate.now().plusDays(2), ShowStatus.CANCELLED);

        mockMvc.perform(delete("/api/admin/movies/{id}", cancelledMovie.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateMovieToEndedRespectsFutureActiveShows() throws Exception {
        String adminToken = tokenFor("admin-update-ended-shows@example.com", Role.ADMIN);
        Movie blockedMovie = saveMovie("Update End Blocked", MovieStatus.NOW_SHOWING, "Drama", "Nepali", LocalDate.now());
        Hall hall = saveHall("Update End Block Hall", Status.ACTIVE);
        saveShow(blockedMovie, hall, LocalDate.now().plusDays(2), ShowStatus.SCHEDULED);

        mockMvc.perform(put("/api/admin/movies/{id}", blockedMovie.getId())
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(movieRequest(
                                "Update End Blocked",
                                MovieStatus.ENDED,
                                120,
                                "https://example.com/poster.jpg",
                                LocalDate.now()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot end movie while future active shows exist"));

        Movie allowedMovie = saveMovie("Update End Allowed", MovieStatus.NOW_SHOWING, "Drama", "Nepali", LocalDate.now());
        mockMvc.perform(put("/api/admin/movies/{id}", allowedMovie.getId())
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(movieRequest(
                                "Update End Allowed",
                                MovieStatus.ENDED,
                                120,
                                "https://example.com/poster.jpg",
                                LocalDate.now()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ENDED"));
    }

    @Test
    void nonAdminsAndUnauthenticatedUsersCannotDeleteMovie() throws Exception {
        Movie movie = saveMovie("Delete Auth Movie", MovieStatus.NOW_SHOWING, "Drama", "Nepali", LocalDate.now());
        String customerToken = tokenFor("customer-delete-movie@example.com", Role.CUSTOMER);
        String staffToken = tokenFor("staff-delete-movie@example.com", Role.STAFF);

        mockMvc.perform(delete("/api/admin/movies/{id}", movie.getId())
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/admin/movies/{id}", movie.getId())
                        .header("Authorization", bearer(staffToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/admin/movies/{id}", movie.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonAdminCannotCreateMovie() throws Exception {
        String customerToken = tokenFor("customer-movie@example.com", Role.CUSTOMER);

        mockMvc.perform(post("/api/admin/movies")
                        .header("Authorization", bearer(customerToken))
                        .contentType("application/json")
                        .content(json(movieRequest("Blocked Movie", MovieStatus.NOW_SHOWING))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void customerStaffAndUnauthenticatedUsersCannotAccessAdminMovieEndpoints() throws Exception {
        String customerToken = tokenFor("customer-admin-movies@example.com", Role.CUSTOMER);
        String staffToken = tokenFor("staff-admin-movies@example.com", Role.STAFF);

        mockMvc.perform(get("/api/admin/movies")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied"));

        mockMvc.perform(get("/api/admin/movies")
                        .header("Authorization", bearer(staffToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied"));

        mockMvc.perform(get("/api/admin/movies"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void movieNotFoundReturnsStandardErrorResponse() throws Exception {
        mockMvc.perform(get("/api/public/movies/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Movie not found with id: 99999"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void dedicatedPublicMovieStatusEndpointsStillReturnLists() throws Exception {
        saveMovie("Now Showing Dedicated", MovieStatus.NOW_SHOWING);
        saveMovie("Upcoming Dedicated", MovieStatus.UPCOMING);

        mockMvc.perform(get("/api/public/movies/now-showing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Now Showing Dedicated"))
                .andExpect(jsonPath("$.data[0].createdAt").doesNotExist());

        mockMvc.perform(get("/api/public/movies/upcoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Upcoming Dedicated"))
                .andExpect(jsonPath("$.data[0].createdAt").doesNotExist());
    }

    @Test
    void adminCanCreateHallAndPublicCanFetchActiveHalls() throws Exception {
        String adminToken = tokenFor("admin-hall@example.com", Role.ADMIN);

        mockMvc.perform(post("/api/admin/halls")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(hallRequest("Hall One", Status.ACTIVE))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Hall created successfully"))
                .andExpect(jsonPath("$.data.name").value("Hall One"));

        mockMvc.perform(get("/api/public/halls/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Active halls fetched successfully"))
                .andExpect(jsonPath("$.data[0].name").value("Hall One"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void publicHallEndpointsExposeOnlyActivePublicContracts() throws Exception {
        Hall activeHall = saveHall("Public Active Hall", Status.ACTIVE);
        Hall inactiveHall = saveHall("Public Inactive Hall", Status.INACTIVE);

        mockMvc.perform(get("/api/public/halls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(activeHall.getId()))
                .andExpect(jsonPath("$.data[0].name").value("Public Active Hall"))
                .andExpect(jsonPath("$.data[0].capacity").value(188))
                .andExpect(jsonPath("$.data[0].layoutRef").doesNotExist())
                .andExpect(jsonPath("$.data[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$.data[0].updatedAt").doesNotExist());

        mockMvc.perform(get("/api/public/halls/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(activeHall.getId()))
                .andExpect(jsonPath("$.data[0].name").value("Public Active Hall"))
                .andExpect(jsonPath("$.data[0].layoutRef").doesNotExist())
                .andExpect(jsonPath("$.data[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$.data[0].updatedAt").doesNotExist());

        mockMvc.perform(get("/api/public/halls/{id}", activeHall.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(activeHall.getId()))
                .andExpect(jsonPath("$.data.name").value("Public Active Hall"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.layoutRef").doesNotExist())
                .andExpect(jsonPath("$.data.createdAt").doesNotExist())
                .andExpect(jsonPath("$.data.updatedAt").doesNotExist());

        mockMvc.perform(get("/api/public/halls/{id}", inactiveHall.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Hall not found with id: " + inactiveHall.getId()));
    }

    @Test
    void adminHallEndpointsExposeAdminContractsAndAllStatuses() throws Exception {
        String adminToken = tokenFor("admin-hall-contract@example.com", Role.ADMIN);
        Hall activeHall = saveHall("Admin Active Hall", Status.ACTIVE);
        Hall inactiveHall = saveHall("Admin Inactive Hall", Status.INACTIVE);

        mockMvc.perform(get("/api/admin/halls")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[*].status", hasItems("ACTIVE", "INACTIVE")))
                .andExpect(jsonPath("$.data[0].layoutRef").value("standard"))
                .andExpect(jsonPath("$.data[0].status").exists())
                .andExpect(jsonPath("$.data[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$.data[0].updatedAt").doesNotExist());

        mockMvc.perform(get("/api/admin/halls/active")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(activeHall.getId()))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));

        mockMvc.perform(get("/api/admin/halls/{id}", inactiveHall.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(inactiveHall.getId()))
                .andExpect(jsonPath("$.data.layoutRef").value("standard"))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.updatedAt").exists());
    }

    @Test
    void nonAdminsCannotUseAdminHallEndpoints() throws Exception {
        String customerToken = tokenFor("customer-hall-denied@example.com", Role.CUSTOMER);
        String staffToken = tokenFor("staff-hall-denied@example.com", Role.STAFF);

        mockMvc.perform(get("/api/admin/halls")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));

        mockMvc.perform(get("/api/admin/halls")
                        .header("Authorization", bearer(staffToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));

        mockMvc.perform(get("/api/admin/halls"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void duplicateHallNameReturnsConflictOrValidationError() throws Exception {
        String adminToken = tokenFor("admin-duplicate-hall@example.com", Role.ADMIN);
        saveHall("Duplicate Hall", Status.ACTIVE);

        mockMvc.perform(post("/api/admin/halls")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(hallRequest("Duplicate Hall", Status.ACTIVE))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void adminCanGenerateSeatLayout() throws Exception {
        String adminToken = tokenFor("admin-seat-layout@example.com", Role.ADMIN);
        Hall hall = saveHall("Layout Hall", Status.ACTIVE);

        mockMvc.perform(post("/api/admin/halls/{hallId}/seat-layout", hall.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Seat layout generated successfully"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void adminCanCreateShowAndSeatsAreGenerated() throws Exception {
        String adminToken = tokenFor("admin-show@example.com", Role.ADMIN);
        Movie movie = saveMovie("Show Movie", MovieStatus.NOW_SHOWING);
        Hall hall = saveHall("Show Hall", Status.ACTIVE);
        postSeatLayout(hall.getId(), adminToken);

        mockMvc.perform(post("/api/admin/shows")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(showRequest(movie.getId(), hall.getId(), 5, "10:00", "12:00"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Show created successfully"))
                .andExpect(jsonPath("$.data.movie.title").value("Show Movie"));

        Long showId = showRepository.findAll().get(0).getId();
        org.assertj.core.api.Assertions.assertThat(seatsForShow(showId)).hasSize(188);
    }

    @Test
    void showCreationRejectsUpcomingEndedInactiveAndOverlappingShows() throws Exception {
        String adminToken = tokenFor("admin-show-validation@example.com", Role.ADMIN);
        Hall activeHall = saveHall("Active Hall", Status.ACTIVE);
        Hall inactiveHall = saveHall("Inactive Hall", Status.INACTIVE);
        Movie nowShowing = saveMovie("Current Movie", MovieStatus.NOW_SHOWING);
        Movie upcoming = saveMovie("Upcoming Movie", MovieStatus.UPCOMING);
        Movie ended = saveMovie("Ended Movie", MovieStatus.ENDED);
        postSeatLayout(activeHall.getId(), adminToken);

        mockMvc.perform(post("/api/admin/shows")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(showRequest(upcoming.getId(), activeHall.getId(), 6, "10:00", "12:00"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(post("/api/admin/shows")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(showRequest(ended.getId(), activeHall.getId(), 6, "13:00", "15:00"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(post("/api/admin/shows")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(showRequest(nowShowing.getId(), inactiveHall.getId(), 6, "16:00", "18:00"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(post("/api/admin/shows")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(showRequest(nowShowing.getId(), activeHall.getId(), 7, "10:00", "12:00"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/admin/shows")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(showRequest(nowShowing.getId(), activeHall.getId(), 7, "11:00", "13:00"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Hall is already booked for another show during this time period. Only one show can be scheduled per hall at a time."))
                .andExpect(jsonPath("$.errors").isArray());
    }

    private Movie saveMovie(String title, MovieStatus status, String genre, String language, LocalDate releaseDate) {
        return movieRepository.save(Movie.builder()
                .title(title)
                .genre(genre)
                .durationMinutes(120)
                .language(language)
                .description("Test movie")
                .posterUrl("https://example.com/poster.jpg")
                .releaseDate(releaseDate)
                .status(status)
                .build());
    }

    private Map<String, Object> movieRequest(
            String title,
            MovieStatus status,
            int durationMinutes,
            String posterUrl,
            LocalDate releaseDate) {
        return Map.of(
                "title", title,
                "genre", "Drama",
                "durationMinutes", durationMinutes,
                "language", "Nepali",
                "description", "Test movie",
                "posterUrl", posterUrl,
                "releaseDate", releaseDate.toString(),
                "status", status.name()
        );
    }

    private void assertInvalidMovieRequest(String adminToken, Map<String, Object> request) throws Exception {
        mockMvc.perform(post("/api/admin/movies")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    private Show saveShow(Movie movie, Hall hall, LocalDate showDate, ShowStatus status) {
        Show show = showRepository.save(Show.builder()
                .movie(movie)
                .hall(hall)
                .showDate(showDate)
                .showTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .build());
        show.setStatus(status);
        return showRepository.save(show);
    }
}
