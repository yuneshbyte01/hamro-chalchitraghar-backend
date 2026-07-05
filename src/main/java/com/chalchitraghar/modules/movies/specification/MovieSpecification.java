package com.chalchitraghar.modules.movies.specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.chalchitraghar.modules.movies.dto.request.MovieSearchCriteria;
import com.chalchitraghar.modules.movies.entity.Movie;
import com.chalchitraghar.modules.movies.enums.MovieStatus;

import jakarta.persistence.criteria.Predicate;

/**
 * Specifications for flexible Movie queries.
 */
public final class MovieSpecification {

    private MovieSpecification() {
    }

    public static Specification<Movie> search(MovieSearchCriteria criteria, MovieStatus status) {
        return search(criteria, status, true);
    }

    public static Specification<Movie> search(MovieSearchCriteria criteria, MovieStatus status, boolean includeEnded) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.search() != null && !criteria.search().isBlank()) {
                String search = "%" + criteria.search().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), search),
                        cb.like(cb.lower(root.get("genre")), search),
                        cb.like(cb.lower(root.get("language")), search)
                ));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            } else if (!includeEnded) {
                predicates.add(cb.notEqual(root.get("status"), MovieStatus.ENDED));
            }
            if (criteria.genre() != null && !criteria.genre().isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("genre")), criteria.genre().trim().toLowerCase()));
            }
            if (criteria.language() != null && !criteria.language().isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("language")), criteria.language().trim().toLowerCase()));
            }
            if (criteria.releaseDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("releaseDate"), criteria.releaseDateFrom()));
            }
            if (criteria.releaseDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("releaseDate"), criteria.releaseDateTo()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
