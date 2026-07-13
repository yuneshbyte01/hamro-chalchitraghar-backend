package com.chalchitraghar.modules.halls.specification;

import com.chalchitraghar.modules.halls.dto.request.HallSearchCriteria;
import com.chalchitraghar.modules.halls.entity.Hall;
import com.chalchitraghar.modules.halls.enums.Status;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** Specifications for flexible Hall queries. */
public final class HallSpecification {

    private HallSpecification() {}

    public static Specification<Hall> publicSearch(HallSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), Status.ACTIVE));

            if (criteria != null && criteria.search() != null && !criteria.search().isBlank()) {
                String search = "%" + criteria.search().trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("name")), search));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Hall> adminSearch(HallSearchCriteria criteria, Status status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria != null && criteria.search() != null && !criteria.search().isBlank()) {
                String search = "%" + criteria.search().trim().toLowerCase() + "%";
                predicates.add(
                        cb.or(
                                cb.like(cb.lower(root.get("name")), search),
                                cb.like(cb.lower(root.get("layoutRef")), search)));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
