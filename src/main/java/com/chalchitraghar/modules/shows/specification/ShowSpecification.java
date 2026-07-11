package com.chalchitraghar.modules.shows.specification;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.data.jpa.domain.Specification;

import com.chalchitraghar.modules.halls.enums.Status;
import com.chalchitraghar.modules.movies.enums.MovieStatus;
import com.chalchitraghar.modules.shows.dto.request.ShowSearchCriteria;
import com.chalchitraghar.modules.shows.entity.Show;
import com.chalchitraghar.modules.shows.enums.ShowStatus;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

/** Specifications for searchable and filterable show listings. */
public final class ShowSpecification {

    private ShowSpecification() {
    }

    public static Specification<Show> search(
            ShowSearchCriteria criteria,
            ShowStatus status,
            boolean publicOnly,
            LocalDate currentDate,
            LocalTime currentTime) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            var movie = root.join("movie", JoinType.INNER);
            var hall = root.join("hall", JoinType.INNER);

            if (criteria.search() != null && !criteria.search().isBlank()) {
                String term = "%" + criteria.search().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(movie.get("title")), term),
                        cb.like(cb.lower(hall.get("name")), term)));
            }
            if (criteria.movieId() != null) {
                predicates.add(cb.equal(movie.get("id"), criteria.movieId()));
            }
            if (criteria.hallId() != null) {
                predicates.add(cb.equal(hall.get("id"), criteria.hallId()));
            }
            if (criteria.showDate() != null) {
                predicates.add(cb.equal(root.get("showDate"), criteria.showDate()));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (publicOnly) {
                predicates.add(cb.equal(movie.get("status"), MovieStatus.NOW_SHOWING));
                predicates.add(cb.equal(hall.get("status"), Status.ACTIVE));
                predicates.add(root.get("status").in(ShowStatus.SCHEDULED, ShowStatus.RUNNING));
                predicates.add(cb.or(
                        cb.greaterThan(root.get("showDate"), currentDate),
                        cb.and(
                                cb.equal(root.get("showDate"), currentDate),
                                cb.greaterThan(root.get("endTime"), currentTime))));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
