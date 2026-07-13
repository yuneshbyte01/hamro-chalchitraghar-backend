package com.chalchitraghar.modules.users.specification;

import com.chalchitraghar.modules.users.dto.request.AdminUserSearchCriteria;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.modules.users.enums.AuthProvider;
import com.chalchitraghar.modules.users.enums.Role;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** Specifications for flexible User queries. */
public final class UserSpecification {

    private UserSpecification() {}

    public static Specification<User> adminSearch(
            AdminUserSearchCriteria criteria, Role role, AuthProvider authProvider) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.search() != null && !criteria.search().isBlank()) {
                String search = "%" + criteria.search().trim().toLowerCase() + "%";
                predicates.add(
                        cb.or(
                                cb.like(cb.lower(root.get("name")), search),
                                cb.like(cb.lower(root.get("email")), search)));
            }

            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }
            if (authProvider != null) {
                predicates.add(cb.equal(root.get("authProvider"), authProvider));
            }
            if (criteria.enabled() != null) {
                predicates.add(cb.equal(root.get("enabled"), criteria.enabled()));
            }
            if (criteria.locked() != null) {
                predicates.add(cb.equal(root.get("locked"), criteria.locked()));
            }
            if (criteria.emailVerified() != null) {
                predicates.add(cb.equal(root.get("emailVerified"), criteria.emailVerified()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
