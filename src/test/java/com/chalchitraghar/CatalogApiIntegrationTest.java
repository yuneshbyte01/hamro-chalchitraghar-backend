package com.chalchitraghar;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.chalchitraghar.modules.halls.entity.Hall;
import com.chalchitraghar.modules.halls.enums.Status;
import com.chalchitraghar.modules.movies.entity.Movie;
import com.chalchitraghar.modules.movies.enums.MovieStatus;
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
                .andExpect(jsonPath("$.data.totalElements").value(2));

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
                .andExpect(jsonPath("$.data.totalElements").value(2));

        mockMvc.perform(get("/api/public/movies").param("language", "Nepali"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));

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
                .andExpect(jsonPath("$.data.content[0].title").value("Old Jatra"))
                .andExpect(jsonPath("$.data.content[1].title").value("Jatra Returns"))
                .andExpect(jsonPath("$.data.content[2].title").value("Silent Hills"));
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
}
