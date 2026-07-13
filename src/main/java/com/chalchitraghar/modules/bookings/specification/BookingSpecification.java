package com.chalchitraghar.modules.bookings.specification;

import com.chalchitraghar.modules.bookings.dto.request.BookingSearchCriteria;
import com.chalchitraghar.modules.bookings.entity.Booking;
import com.chalchitraghar.modules.bookings.enums.BookingStatus;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** Database-backed booking search and audience filtering. */
public final class BookingSpecification {
    private BookingSpecification() {}

    public static Specification<Booking> search(
            BookingSearchCriteria criteria, BookingStatus status, Long ownerId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            var user = root.join("user", JoinType.INNER);
            var show = root.join("show", JoinType.INNER);
            var movie = show.join("movie", JoinType.INNER);
            var hall = show.join("hall", JoinType.INNER);

            if (ownerId != null) predicates.add(cb.equal(user.get("id"), ownerId));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (criteria.showId() != null)
                predicates.add(cb.equal(show.get("id"), criteria.showId()));
            if (criteria.movieId() != null)
                predicates.add(cb.equal(movie.get("id"), criteria.movieId()));
            if (criteria.hallId() != null)
                predicates.add(cb.equal(hall.get("id"), criteria.hallId()));
            if (criteria.customerId() != null)
                predicates.add(cb.equal(user.get("id"), criteria.customerId()));
            if (criteria.showDateFrom() != null)
                predicates.add(
                        cb.greaterThanOrEqualTo(show.get("showDate"), criteria.showDateFrom()));
            if (criteria.showDateTo() != null)
                predicates.add(cb.lessThanOrEqualTo(show.get("showDate"), criteria.showDateTo()));
            if (criteria.bookingTimeFrom() != null)
                predicates.add(
                        cb.greaterThanOrEqualTo(
                                root.get("bookingTime"), criteria.bookingTimeFrom()));
            if (criteria.bookingTimeTo() != null)
                predicates.add(
                        cb.lessThanOrEqualTo(root.get("bookingTime"), criteria.bookingTimeTo()));
            if (criteria.search() != null && !criteria.search().isBlank()) {
                String term = "%" + criteria.search().trim().toLowerCase() + "%";
                predicates.add(
                        cb.or(
                                cb.like(cb.lower(user.get("name")), term),
                                cb.like(cb.lower(user.get("email")), term),
                                cb.like(cb.lower(movie.get("title")), term),
                                cb.like(cb.lower(hall.get("name")), term)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
