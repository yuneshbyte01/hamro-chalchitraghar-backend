package com.chalchitraghar;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
                .andExpect(jsonPath("$.data[0].title").value("Public Movie"))
                .andExpect(jsonPath("$.data[0].description").doesNotExist())
                .andExpect(jsonPath("$.data[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$.data[0].updatedAt").doesNotExist())
                .andExpect(jsonPath("$.errors").isArray());
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
                .andExpect(jsonPath("$.data[0].title").value("Admin Movie List"))
                .andExpect(jsonPath("$.data[0].genre").value("Drama"))
                .andExpect(jsonPath("$.data[0].language").value("Nepali"))
                .andExpect(jsonPath("$.data[0].durationMinutes").doesNotExist())
                .andExpect(jsonPath("$.data[0].description").doesNotExist())
                .andExpect(jsonPath("$.data[0].posterUrl").doesNotExist())
                .andExpect(jsonPath("$.data[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$.data[0].updatedAt").doesNotExist());
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
}
