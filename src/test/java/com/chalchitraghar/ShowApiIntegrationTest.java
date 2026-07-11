package com.chalchitraghar;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import com.chalchitraghar.modules.halls.entity.Hall;
import com.chalchitraghar.modules.halls.enums.Status;
import com.chalchitraghar.modules.movies.entity.Movie;
import com.chalchitraghar.modules.movies.enums.MovieStatus;
import com.chalchitraghar.modules.shows.entity.Show;
import com.chalchitraghar.modules.shows.enums.ShowStatus;
import com.chalchitraghar.modules.users.enums.Role;

class ShowApiIntegrationTest extends AbstractIntegrationTest {

    @Test
    void publicListsUseSummaryContractAndApplyVisibilityRules() throws Exception {
        Movie current = saveMovie("Visible Movie", MovieStatus.NOW_SHOWING);
        Movie ended = saveMovie("Ended Movie", MovieStatus.ENDED);
        Hall active = saveHall("Active Hall", Status.ACTIVE);
        Hall inactive = saveHall("Inactive Hall", Status.INACTIVE);
        saveShow(current, active, ShowStatus.SCHEDULED, 10);
        saveShow(current, active, ShowStatus.CANCELLED, 11);
        saveShow(current, active, ShowStatus.COMPLETED, 12);
        saveShow(ended, active, ShowStatus.SCHEDULED, 13);
        saveShow(current, inactive, ShowStatus.SCHEDULED, 14);

        mockMvc.perform(get("/api/public/shows"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].movieTitle").value("Visible Movie"))
                .andExpect(jsonPath("$.data.content[0].hallName").value("Active Hall"))
                .andExpect(jsonPath("$.data.content[0].movie").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].hall").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].updatedAt").doesNotExist())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(get("/api/public/shows/movie/{movieId}", current.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1));
        mockMvc.perform(get("/api/public/shows").param("movieId", current.getId().toString())
                        .param("date", LocalDate.now().plusDays(10).toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void publicDetailUsesPublicNestedContractsAndHidesIneligibleShows() throws Exception {
        Movie current = saveMovie("Public Detail Movie", MovieStatus.NOW_SHOWING);
        Hall active = saveHall("Public Detail Hall", Status.ACTIVE);
        Show visible = saveShow(current, active, ShowStatus.RUNNING, 10);

        mockMvc.perform(get("/api/public/shows/{id}", visible.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.movie.title").value("Public Detail Movie"))
                .andExpect(jsonPath("$.data.movie.createdAt").doesNotExist())
                .andExpect(jsonPath("$.data.hall.layoutRef").doesNotExist())
                .andExpect(jsonPath("$.data.createdAt").doesNotExist());

        for (Show hidden : new Show[] {
                saveShow(current, active, ShowStatus.CANCELLED, 11),
                saveShow(current, active, ShowStatus.COMPLETED, 12),
                saveShow(saveMovie("Historical Movie", MovieStatus.ENDED), active, ShowStatus.SCHEDULED, 13),
                saveShow(current, saveHall("Historical Hall", Status.INACTIVE), ShowStatus.SCHEDULED, 14) }) {
            mockMvc.perform(get("/api/public/shows/{id}", hidden.getId())).andExpect(status().isNotFound());
        }
        mockMvc.perform(get("/api/public/shows/{id}", Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    void adminListAndDetailExposeEveryHistoricalStatus() throws Exception {
        String token = tokenFor("show-admin@example.com", Role.ADMIN);
        Movie ended = saveMovie("Admin Historical Movie", MovieStatus.ENDED);
        Hall inactive = saveHall("Admin Historical Hall", Status.INACTIVE);
        Show scheduled = saveShow(ended, inactive, ShowStatus.SCHEDULED, 10);
        Show running = saveShow(ended, inactive, ShowStatus.RUNNING, 11);
        Show completed = saveShow(ended, inactive, ShowStatus.COMPLETED, 12);
        Show cancelled = saveShow(ended, inactive, ShowStatus.CANCELLED, 13);

        mockMvc.perform(get("/api/admin/shows").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].status", containsInAnyOrder("SCHEDULED", "RUNNING", "COMPLETED", "CANCELLED")))
                .andExpect(jsonPath("$.data.content[0].movieTitle").exists())
                .andExpect(jsonPath("$.data.content[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$.data.totalElements").value(4));

        for (Show show : new Show[] {scheduled, running, completed, cancelled}) {
            mockMvc.perform(get("/api/admin/shows/{id}", show.getId()).header("Authorization", bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.createdAt").exists())
                    .andExpect(jsonPath("$.data.updatedAt").exists())
                    .andExpect(jsonPath("$.data.movie.createdAt").exists())
                    .andExpect(jsonPath("$.data.hall.layoutRef").value("standard"));
        }
        mockMvc.perform(get("/api/admin/shows/{id}", Long.MAX_VALUE).header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminShowEndpointsRejectNonAdminsAndAnonymousUsers() throws Exception {
        for (Role role : new Role[] {Role.CUSTOMER, Role.STAFF}) {
            String token = tokenFor("show-" + role.name().toLowerCase() + "@example.com", role);
            mockMvc.perform(get("/api/admin/shows").header("Authorization", bearer(token)))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/admin/shows")).andExpect(status().isUnauthorized());
    }

    @Test
    void createAndUpdateReturnAdminDetailAndKeepSeatGeneration() throws Exception {
        String token = tokenFor("show-write-admin@example.com", Role.ADMIN);
        Movie movie = saveMovie("Writable Show", MovieStatus.NOW_SHOWING);
        Hall hall = saveHall("Writable Hall", Status.ACTIVE);
        postSeatLayout(hall.getId(), token);

        var created = mockMvc.perform(post("/api/admin/shows")
                        .header("Authorization", bearer(token)).contentType("application/json")
                        .content(json(showRequest(movie.getId(), hall.getId(), 10, "10:00", "12:00"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.movie.createdAt").exists())
                .andExpect(jsonPath("$.data.hall.layoutRef").value("standard"))
                .andExpect(jsonPath("$.data.createdAt").exists()).andReturn();
        Long showId = objectMapper.readTree(created.getResponse().getContentAsString()).path("data").path("id").asLong();
        org.assertj.core.api.Assertions.assertThat(seatsForShow(showId)).hasSize(188);

        mockMvc.perform(put("/api/admin/shows/{id}", showId)
                        .header("Authorization", bearer(token)).contentType("application/json")
                        .content(json(showRequest(movie.getId(), hall.getId(), 11, "11:00", "13:00"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.movie.title").value("Writable Show"))
                .andExpect(jsonPath("$.data.updatedAt").exists());
    }

    @Test
    void publicListSupportsPagingSearchFiltersSortingAndValidation() throws Exception {
        Movie firstMovie = saveMovie("Kabaddi Search", MovieStatus.NOW_SHOWING);
        Movie secondMovie = saveMovie("Different Movie", MovieStatus.NOW_SHOWING);
        Hall firstHall = saveHall("Aud1 Main", Status.ACTIVE);
        Hall secondHall = saveHall("Balcony Hall", Status.ACTIVE);
        saveShow(firstMovie, firstHall, ShowStatus.SCHEDULED, 12);
        saveShow(firstMovie, secondHall, ShowStatus.RUNNING, 11);
        saveShow(secondMovie, secondHall, ShowStatus.SCHEDULED, 10);

        mockMvc.perform(get("/api/public/shows").param("page", "1").param("size", "1")
                        .param("sortBy", "showDate").param("sortDir", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.content[0].showDate")
                        .value(LocalDate.now().plusDays(11).toString()));

        mockMvc.perform(get("/api/public/shows").param("search", "kAbAdDi"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(2));
        mockMvc.perform(get("/api/public/shows").param("search", "AUD1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));
        mockMvc.perform(get("/api/public/shows")
                        .param("movieId", firstMovie.getId().toString())
                        .param("hallId", secondHall.getId().toString())
                        .param("showDate", LocalDate.now().plusDays(11).toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(get("/api/public/shows").param("sortBy", "movie"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        mockMvc.perform(get("/api/public/shows").param("sortDir", "sideways"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void adminListSupportsCombinedFiltersSearchSortingAndPaging() throws Exception {
        String token = tokenFor("show-query-admin@example.com", Role.ADMIN);
        Movie movie = saveMovie("Admin Search Movie", MovieStatus.ENDED);
        Movie otherMovie = saveMovie("Other Historical Movie", MovieStatus.ENDED);
        Hall hall = saveHall("Admin Search Hall", Status.INACTIVE);
        Hall otherHall = saveHall("Other Historical Hall", Status.INACTIVE);
        saveShow(movie, hall, ShowStatus.CANCELLED, 12);
        saveShow(movie, hall, ShowStatus.COMPLETED, 11);
        saveShow(otherMovie, otherHall, ShowStatus.SCHEDULED, 10);

        mockMvc.perform(get("/api/admin/shows").header("Authorization", bearer(token))
                        .param("search", "admin search")
                        .param("movieId", movie.getId().toString())
                        .param("hallId", hall.getId().toString())
                        .param("status", "cancelled")
                        .param("showDate", LocalDate.now().plusDays(12).toString())
                        .param("sortBy", "showTime").param("sortDir", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("CANCELLED"));

        mockMvc.perform(get("/api/admin/shows").header("Authorization", bearer(token))
                        .param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.last").value(true));
    }

    @Test
    void schedulingValidatesDatesTimesDurationBufferAndCancelledOverlap() throws Exception {
        String token = tokenFor("show-schedule-admin@example.com", Role.ADMIN);
        Movie movie = saveMovie("Schedule Rules Movie", MovieStatus.NOW_SHOWING);
        Hall hall = saveHall("Schedule Rules Hall", Status.ACTIVE);
        postSeatLayout(hall.getId(), token);

        LocalTime futureStart = LocalTime.now().plusHours(1).withSecond(0).withNano(0);
        if (!futureStart.plusHours(2).isAfter(futureStart)) {
            futureStart = LocalTime.of(20, 0);
        }
        mockMvc.perform(post("/api/admin/shows").header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content(json(scheduleRequest(movie, hall, LocalDate.now(), futureStart, futureStart.plusHours(2)))))
                .andExpect(status().isCreated());

        assertScheduleRejected(token, movie, hall, LocalDate.now().minusDays(1), LocalTime.of(10, 0), LocalTime.of(12, 0));
        assertScheduleRejected(token, movie, hall, LocalDate.now(), LocalTime.now().minusMinutes(1), LocalTime.now().plusMinutes(119));
        assertScheduleRejected(token, movie, hall, LocalDate.now().plusDays(20), LocalTime.of(14, 0), LocalTime.of(14, 0));
        assertScheduleRejected(token, movie, hall, LocalDate.now().plusDays(20), LocalTime.of(14, 0), LocalTime.of(13, 59));

        Hall durationHall = saveHall("Duration Rules Hall", Status.ACTIVE);
        postSeatLayout(durationHall.getId(), token);
        mockMvc.perform(post("/api/admin/shows").header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content(json(scheduleRequest(movie, durationHall, LocalDate.now().plusDays(21),
                                LocalTime.of(10, 0), LocalTime.of(12, 5)))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/admin/shows").header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content(json(scheduleRequest(movie, durationHall, LocalDate.now().plusDays(22),
                                LocalTime.of(10, 0), LocalTime.of(12, 6)))))
                .andExpect(status().isConflict());

        Show existing = saveShow(movie, hall, ShowStatus.SCHEDULED, 30);
        existing.setShowTime(LocalTime.of(10, 0));
        existing.setEndTime(LocalTime.of(12, 0));
        showRepository.save(existing);
        assertConflict(token, movie, hall, 30, LocalTime.of(11, 0), LocalTime.of(13, 0));
        assertConflict(token, movie, hall, 30, LocalTime.of(12, 14), LocalTime.of(14, 14));

        Hall bufferHall = saveHall("Buffer Gap Hall", Status.ACTIVE);
        postSeatLayout(bufferHall.getId(), token);
        Show buffered = saveShow(movie, bufferHall, ShowStatus.SCHEDULED, 31);
        buffered.setShowTime(LocalTime.of(10, 0));
        buffered.setEndTime(LocalTime.of(12, 0));
        showRepository.save(buffered);
        mockMvc.perform(post("/api/admin/shows").header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content(json(scheduleRequest(movie, bufferHall, LocalDate.now().plusDays(31),
                                LocalTime.of(12, 15), LocalTime.of(14, 15)))))
                .andExpect(status().isCreated());

        Hall cancelledHall = saveHall("Cancelled Gap Hall", Status.ACTIVE);
        postSeatLayout(cancelledHall.getId(), token);
        Show cancelled = saveShow(movie, cancelledHall, ShowStatus.CANCELLED, 32);
        cancelled.setShowTime(LocalTime.of(10, 0));
        cancelled.setEndTime(LocalTime.of(12, 0));
        showRepository.save(cancelled);
        mockMvc.perform(post("/api/admin/shows").header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content(json(scheduleRequest(movie, cancelledHall, LocalDate.now().plusDays(32),
                                LocalTime.of(10, 0), LocalTime.of(12, 0)))))
                .andExpect(status().isCreated());
    }

    @Test
    void statusEndpointEnforcesLifecycleAndScheduleUpdateRestrictions() throws Exception {
        String token = tokenFor("show-lifecycle-admin@example.com", Role.ADMIN);
        Movie movie = saveMovie("Lifecycle Movie", MovieStatus.NOW_SHOWING);
        Hall hall = saveHall("Lifecycle Hall", Status.ACTIVE);
        Show runningPath = saveShow(movie, hall, ShowStatus.SCHEDULED, 40);

        patchStatus(token, runningPath.getId(), "RUNNING", 200);
        mockMvc.perform(put("/api/admin/shows/{id}", runningPath.getId())
                        .header("Authorization", bearer(token)).contentType("application/json")
                        .content(json(showRequest(movie.getId(), hall.getId(), 41, "10:00", "12:00"))))
                .andExpect(status().isConflict());
        patchStatus(token, runningPath.getId(), "COMPLETED", 200);
        patchStatus(token, runningPath.getId(), "CANCELLED", 409);

        Show scheduledCancel = saveShow(movie, hall, ShowStatus.SCHEDULED, 42);
        patchStatus(token, scheduledCancel.getId(), "CANCELLED", 200);
        patchStatus(token, scheduledCancel.getId(), "RUNNING", 409);

        Show runningCancel = saveShow(movie, hall, ShowStatus.RUNNING, 43);
        patchStatus(token, runningCancel.getId(), "CANCELLED", 200);

        Show scheduledUpdate = saveShow(movie, hall, ShowStatus.SCHEDULED, 44);
        mockMvc.perform(put("/api/admin/shows/{id}", scheduledUpdate.getId())
                        .header("Authorization", bearer(token)).contentType("application/json")
                        .content(json(showRequest(movie.getId(), hall.getId(), 45, "10:00", "12:00"))))
                .andExpect(status().isOk());
        for (Show terminal : new Show[] {
                saveShow(movie, hall, ShowStatus.COMPLETED, 46),
                saveShow(movie, hall, ShowStatus.CANCELLED, 47) }) {
            mockMvc.perform(put("/api/admin/shows/{id}", terminal.getId())
                            .header("Authorization", bearer(token)).contentType("application/json")
                            .content(json(showRequest(movie.getId(), hall.getId(), 48, "10:00", "12:00"))))
                    .andExpect(status().isConflict());
        }
    }

    private java.util.Map<String, Object> scheduleRequest(
            Movie movie, Hall hall, LocalDate date, LocalTime start, LocalTime end) {
        return java.util.Map.of("movieId", movie.getId(), "hallId", hall.getId(),
                "showDate", date.toString(), "showTime", start.toString(), "endTime", end.toString());
    }

    private void assertScheduleRejected(String token, Movie movie, Hall hall, LocalDate date, LocalTime start, LocalTime end)
            throws Exception {
        mockMvc.perform(post("/api/admin/shows").header("Authorization", bearer(token))
                        .contentType("application/json").content(json(scheduleRequest(movie, hall, date, start, end))))
                .andExpect(status().isBadRequest());
    }

    private void assertConflict(String token, Movie movie, Hall hall, int day, LocalTime start, LocalTime end)
            throws Exception {
        mockMvc.perform(post("/api/admin/shows").header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content(json(scheduleRequest(movie, hall, LocalDate.now().plusDays(day), start, end))))
                .andExpect(status().isConflict());
    }

    private void patchStatus(String token, Long showId, String target, int expectedStatus) throws Exception {
        mockMvc.perform(patch("/api/admin/shows/{id}/status", showId)
                        .header("Authorization", bearer(token)).contentType("application/json")
                        .content(json(java.util.Map.of("status", target))))
                .andExpect(status().is(expectedStatus));
    }

    private Show saveShow(Movie movie, Hall hall, ShowStatus status, int daysFromNow) {
        Show show = showRepository.save(Show.builder()
                .movie(movie).hall(hall).showDate(LocalDate.now().plusDays(daysFromNow))
                .showTime(LocalTime.of(10, 0)).endTime(LocalTime.of(12, 0)).build());
        show.setStatus(status);
        return showRepository.save(show);
    }
}
